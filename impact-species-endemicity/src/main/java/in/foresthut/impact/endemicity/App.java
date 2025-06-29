package in.foresthut.impact.endemicity;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.endemicity.infra.EcoregionClient;
import in.foresthut.impact.endemicity.infra.GeminiClient;
import in.foresthut.impact.endemicity.infra.SpeciesEndemicityDatabaseConfig;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private static MongoDatabase speciesHabitatDb = SpeciesEndemicityDatabaseConfig.getInstance().database();
	private static MongoCollection<Document> dataCollection;
	private final static Config config = Config.getInstance();
	private static Map<String, String> ecoregionCache = new HashMap<>();
	private final static String SOURCE_QUEUE_NAME = config.get("rabbitmq.source.queue.name");
	private final static String TARGET_QUEUE_NAME = config.get("rabbitmq.target.queue.name");
	private final static int NUMBER_OF_CONSUMERS = 4;

	public static void main(String[] args) throws IOException {
		GeminiClient geminiClient = GeminiClient.getInstance();
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.source.host"));
		Connection connection = null;
		try {
			connection = factory.newConnection();

			// Creating multiple consumers
			for (int i = 0; i < NUMBER_OF_CONSUMERS; i++) {
				createConsumers(connection, i + 1, geminiClient);
			}
			logger.info("Started service '{}' successfully.", config.get("service.name"));
		} catch (IOException | TimeoutException e) {
			logger.error("Error while connecting to the queue host {}", config.get("rabbitmq.source.host"), e);
			return;
		}
	}

	private static void createConsumers(Connection connection, int consumerId, GeminiClient geminiClient)
			throws IOException {
		// Create a channel for each consumer
		Channel channel = connection.createChannel();
		channel.basicQos(1);

		// Declare the queue (if it doesn't exist)
		channel.queueDeclare(SOURCE_QUEUE_NAME, false, false, false, null);

		// Create a consumer
		boolean autoAck = false;
		channel.basicConsume(SOURCE_QUEUE_NAME, autoAck, String.valueOf("Consumer" + consumerId),
				new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
							byte[] body) throws IOException {
						long deliveryTag = envelope.getDeliveryTag();
						var observationMessage = new ObjectMapper().readValue(body, ObservationMessage.class);
						logger.info("Consumer {} received {}", consumerId, observationMessage);

						// Check if species exists in endemicity Db
						String regionPrompt = fetchRegionName(observationMessage.ecoregionId());
						dataCollection = speciesHabitatDb.getCollection(regionPrompt.toLowerCase().replace(' ', '_'));
						Document speciesDocument = dataCollection.find(
								new BsonDocument("species", new BsonString(observationMessage.observation().species())))
								.first();

						boolean endemicity = false;
						if (speciesDocument == null) {
							try {
								String promptText = "Is " + observationMessage.observation().species() + " endemic to "
										+ regionPrompt + "?";
								String response = geminiClient.send(promptText);
								endemicity = parse(response);

								// Add species to Db
								dataCollection
										.insertOne(new Document("species", observationMessage.observation().species())
												.append("endemic", endemicity));
								publishMessage(channel, deliveryTag, observationMessage, endemicity);

							} catch (Exception e) {
								channel.basicNack(deliveryTag, false, true);
								logger.error("Error while processing the message from queue {}", SOURCE_QUEUE_NAME, e);
							}
						} else {
							endemicity = speciesDocument.getBoolean("endemic");
							publishMessage(channel, deliveryTag, observationMessage, endemicity);
						}
					}

					private String fetchRegionName(String ecoregionId) {
						try {
							if (ecoregionCache.isEmpty()) {
								String region = null;
								EcoregionClient ecoClient = new EcoregionClient();
								var ecoregions = ecoClient.ecoregions();
								for (var ecoregion : ecoregions) {
									ecoregionCache.put(ecoregionId, ecoregion.region());
									if (ecoregion.id().equals(ecoregionId))
										region = ecoregion.region();
								}
								return region;
							} else {
								return ecoregionCache.get(ecoregionId);
							}
						} catch (IOException | InterruptedException e) {
							logger.error("Error fetching ecoregions", e);
						}
						return null;
					}

					private void publishMessage(Channel channel, long deliveryTag,
							ObservationMessage observationMessage, boolean endemicity) throws IOException {
						SpeciesMessage speciesMessage;
						// Add ecoregionId to message and publish
						speciesMessage = new SpeciesMessage(observationMessage.ecoregionId(),
								toSpecies(observationMessage.observation(), endemicity));

						try {
							publishMessage(speciesMessage);
							channel.basicAck(deliveryTag, false);
						} catch (IOException | TimeoutException e) {
							channel.basicNack(deliveryTag, false, true);
							logger.error("Error while publishing a message {} to {}", speciesMessage, TARGET_QUEUE_NAME,
									e);
						}
					}

					private Species toSpecies(Observation observation, boolean endemicity) {
						return new Species(observation.species(), observation.kingdom(), observation.phylum(),
								observation._class(), observation.order(), observation.family(),
								observation.iucnRedListCategory(), List.of(), false, false, endemicity, false, 1);
					}

					private boolean parse(String response) {
						boolean isEndemic = false;
						Pattern pattern = Pattern.compile("Yes|is endemic|No|not endemic", Pattern.CASE_INSENSITIVE);
						Matcher matcher = pattern.matcher(response);

						if (matcher.find()) {
							isEndemic = matcher.group().toLowerCase().matches("yes|is endemic");
						}
						return isEndemic;
					}

					private void publishMessage(SpeciesMessage msg) throws IOException, TimeoutException {
						ConnectionFactory factory = new ConnectionFactory();
						factory.setHost(config.get("rabbitmq.target.host"));
						try (Connection connection = factory.newConnection();
								Channel channel = connection.createChannel()) {
							channel.queueDeclare(TARGET_QUEUE_NAME, false, false, false, null);
							var message = new ObjectMapper().writeValueAsBytes(msg);
							channel.basicPublish("", TARGET_QUEUE_NAME, null, message);
						} catch (IOException e) {
							logger.error("{}", e);
							throw e;
						} catch (TimeoutException e1) {
							logger.error("{}", e1);
							throw e1;
						}
					}
				});

		logger.info("Consumer " + consumerId + " is waiting for messages.");
	}

	static record SpeciesMessage(String ecoregionId, Species species) {
	}

	static record Species(String species, String kingdom, String phylum, String _class, String order, String family,
			String iucnStatus, List<String> habitats, boolean keySpecies, boolean invasive, boolean endemic,
			boolean apexPredator, int observationCount) {
	}

	static record ObservationMessage(String ecoregionId, Observation observation) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static record Observation(long key, String species, String kingdom, String phylum,
			@JsonProperty(value = "class") String _class, String order, String family, String iucnRedListCategory,
			double decimalLatitude, double decimalLongitude, String eventDate, String modified, String datasetName,
			String recordedBy) {
	}
}
