package in.foresthut.impact.observation;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.obseravation.infra.ObservationDBConfiguration;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private static MongoDatabase observationDatabase;
	private static MongoCollection<Document> observationCollection;
	private final static Config config = Config.getInstance();
	private final static String OBSERVATIONS_QUEUE_NAME = config.get("rabbitmq.observations.queue.name");
	private final static String EXCHANGE_NAME = config.get("rabbitmq.exchange.name");
	private final static int NUMBER_OF_CONSUMERS = 4;

	public static void main(String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		observationDatabase = ObservationDBConfiguration.getInstance();

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.observations.host"));
		Connection connection = factory.newConnection();

		// Creating multiple consumers
		for (int i = 0; i < NUMBER_OF_CONSUMERS; i++) {
			createConsumers(connection, i + 1, observationDatabase);
		}
	}

	private static void createConsumers(Connection connection, int consumerId, MongoDatabase observationDatabase)
			throws IOException {
		// Create a channel for each consumer
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
		channel.basicQos(1);

		// Bind the queue with exchange
		channel.queueBind(OBSERVATIONS_QUEUE_NAME, EXCHANGE_NAME, "");

		// Create a consumer
		boolean autoAck = false;
		channel.basicConsume(OBSERVATIONS_QUEUE_NAME, autoAck, String.valueOf("Consumer" + consumerId),
				new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
							byte[] body) throws IOException {
						long deliveryTag = envelope.getDeliveryTag();
						ObservationMessage observationMsg = new ObjectMapper().readValue(body,
								ObservationMessage.class);
						logger.info("Consumer {} received {}", consumerId, observationMsg);

						// Check for a duplicate observation in observation Db
						String ecoregionId = observationMsg.ecoregionId;
						observationCollection = observationDatabase.getCollection("_" + ecoregionId);
						long gbifObservationId = observationMsg.observation().key();
						long count = observationCollection
								.countDocuments(new BsonDocument("key", new BsonInt64(gbifObservationId)));

						if (count == 0) {
							// 1. Add obervation to db
							observationCollection.insertOne(toDocument(observationMsg.observation()));
							// 2. Ack observation queue to confirm message processing
							channel.basicAck(deliveryTag, false);
							logger.info("Inserted observation with GBIF Observation key {} in Observation db",
									gbifObservationId);
						} else {
							logger.info("Duplicate message for GBIF Observation key {}", gbifObservationId);
							// 1. Update observartion in db
							Bson filter = Filters.eq("gbifKey", gbifObservationId);
							Document newObservation = toDocument(observationMsg.observation());
							observationCollection.findOneAndReplace(filter, newObservation);
							// 2. Ack observation queue to confirm message processing
							channel.basicAck(deliveryTag, false);
							logger.info("Updated observation with GBIF Observation key {} in Observation db",
									gbifObservationId);
						}
					}
				});

		logger.info("Consumer " + consumerId + " is waiting for messages.");
	}

	static Document toDocument(Observation observation) {
		Document geoJsonPoint = new Document("type", "Point").append("coordinates",
				List.of(observation.decimalLongitude(), observation.decimalLatitude()));
		return new Document("gbifKey", observation.key()).append("species", observation.species())
				.append("kingdom", observation.kingdom()).append("phylum", observation.phylum())
				.append("_class", observation._class()).append("order", observation.order())
				.append("family", observation.family()).append("iucnStatus", observation.iucnRedListCategory())
				.append("location", geoJsonPoint).append("eventDate", observation.eventDate())
				.append("modifiedOn", observation.modified()).append("csisName", observation.datasetName())
				.append("recordedBy", observation.recordedBy());
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
