package in.foresthut.impact.species;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.species.infa.SpeciesDBConfiguration;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private static MongoDatabase speciesDatabase;
	private static MongoCollection<Document> speciesCollection;
	private final static Config config = Config.getInstance();
	private final static String SPECIES_QUEUE_NAME = config.get("rabbitmq.species.queue.name");
	private final static String EXCHANGE_NAME = config.get("rabbitmq.exchange.name");
	private final static int NUMBER_OF_CONSUMERS = 4;

	public static void main(String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		speciesDatabase = SpeciesDBConfiguration.getInstance().database();

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.observations.host"));
		Connection connection = factory.newConnection();

		// Creating multiple consumers
		for (int i = 0; i < NUMBER_OF_CONSUMERS; i++) {
			createConsumers(connection, i + 1, speciesDatabase);
		}
	}

	private static void createConsumers(Connection connection, int consumerId, MongoDatabase observationDatabase)
			throws IOException {
		// Create a channel for each consumer
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
		channel.basicQos(1);

		// Bind the queue with the exchange
		channel.queueBind(SPECIES_QUEUE_NAME, EXCHANGE_NAME, "");

		// Create a consumer
		boolean autoAck = false;
		channel.basicConsume(SPECIES_QUEUE_NAME, autoAck, String.valueOf("Consumer" + consumerId),
				new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
							byte[] body) throws IOException {
						long deliveryTag = envelope.getDeliveryTag();
						ObservationMessage observationMsg = new ObjectMapper().readValue(body,
								ObservationMessage.class);
						logger.info("Consumer {} received {}", consumerId, observationMsg);

						// Check for a duplicate species in species Db
						String ecoregionId = observationMsg.ecoregionId;
						speciesCollection = observationDatabase.getCollection("_" + ecoregionId);
						String speciesName = observationMsg.observation().species();
						long count = speciesCollection
								.countDocuments(new BsonDocument("species", new BsonString(speciesName)));

						if (count == 0) {
							// 1. Add species to db
							speciesCollection.insertOne(toDocument(observationMsg.observation()));
							// 2. Ack species queue to confirm message processing
							channel.basicAck(deliveryTag, false);
							logger.info("Inserted species {} in Species db", speciesName);
						} else {
							logger.info("Species {} already exists.", speciesName);
							// 1. Update observartion count in db atomically
							int newObservationCount = increaseObservationCount(speciesCollection, speciesName);
							// 2. Ack observation queue to confirm message processing
							channel.basicAck(deliveryTag, false);
							logger.info("Updated species {} count in db to {}", speciesName, newObservationCount);
						}
					}
				});

		logger.info("Consumer " + consumerId + " is waiting for messages.");
	}

	static Document toDocument(Observation observation) {
		return new Document("species", observation.species()).append("kingdom", observation.kingdom())
				.append("phylum", observation.phylum()).append("_class", observation._class())
				.append("order", observation.order()).append("family", observation.family())
				.append("iucnStatus", observation.iucnRedListCategory()).append("habitats", List.of())
				.append("keystone", false).append("invasive", false).append("introduced", false)
				.append("apexPredator", false).append("observationCount", 1);
	}

	static int increaseObservationCount(MongoCollection<Document> speciesCollection, String speciesName) {
		SpeciesDBConfiguration speciesDBConfig = SpeciesDBConfiguration.getInstance();
		var mongoClient = speciesDBConfig.mongoClient();

		// Create a session
		ClientSession session = mongoClient.startSession();

		// Start a transaction
		session.startTransaction();

		int observationCount = 0;
		try {
			Bson filter = Filters.eq("species", speciesName);
			Document species = speciesCollection.find(filter).first();
			observationCount = species.getInteger("observationCount");
			Bson updateObservationCount = Updates.set("observationCount", ++observationCount);
			speciesCollection.updateOne(filter, updateObservationCount);
		} catch (Exception e) {
			session.abortTransaction();
			logger.error("Transaction aborted due to error {}", e.getMessage(), e);
		} finally {
			session.close();
		}
		return observationCount;
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
