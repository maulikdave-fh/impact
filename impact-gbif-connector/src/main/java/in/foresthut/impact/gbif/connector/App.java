package in.foresthut.impact.gbif.connector;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.gbif.connector.infra.CSISProcessedMessagesDBConfiguration;
import in.foresthut.impact.gbif.connector.infra.GBIFClient;
import in.foresthut.impact.gbif.connector.infra.GBIFClient.Occurrence;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private static MongoDatabase csisProcessedMessagesDB = CSISProcessedMessagesDBConfiguration.getInstance();
	private static MongoCollection<Document> processedMessageCollection;
	private final static Config config = Config.getInstance();
	private final static String TASK_QUEUE_NAME = config.get("rabbitmq.csis-tasks.queue.name");
	private final static String EXCHNAGE_NAME = config.get("rabbitmq.exchange.name");
	private final static String ROUNTING_KEY = "dailyRun";
	private final static int NUMBER_OF_CONSUMERS = 4;

	public static void main(String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		processedMessageCollection = csisProcessedMessagesDB.getCollection("messageIds");

		GBIFClient gbifClient = GBIFClient.getInstance();

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.csis-tasks.host"));
		Connection connection = factory.newConnection();

		// Creating multiple consumers
		for (int i = 0; i < NUMBER_OF_CONSUMERS; i++) {
			createConsumers(connection, i + 1, gbifClient);
		}
	}

	private static void createConsumers(Connection connection, int consumerId, GBIFClient gbifClient)
			throws IOException {
		// Create a channel for each consumer
		Channel channel = connection.createChannel();
		channel.basicQos(1);

		// Declare the queue (if it doesn't exist)
		channel.queueDeclare(TASK_QUEUE_NAME, false, false, false, null);

		// Create a consumer
		boolean autoAck = false;
		channel.basicConsume(TASK_QUEUE_NAME, autoAck, String.valueOf("Consumer" + consumerId),
				new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
							byte[] body) throws IOException {
						long deliveryTag = envelope.getDeliveryTag();
						Task message = new ObjectMapper().readValue(body, Task.class);
						logger.info("Consumer {} received {}", consumerId, message);

						// Check for a duplicate message in consumer Db
						int hash = message.messageHash();
						long count = processedMessageCollection
								.countDocuments(new BsonDocument("messageId", new BsonInt32(hash)));

						if (count == 0) {
							try {
								List<Occurrence> observations = gbifClient.occurrences(message);
								logger.info("Received {} observations from GBIF for ecoregionId {} and hash {}",
										observations.size(), message.ecoregionId(), message.messageHash());
								// 1. Put observations in Observations QUEUE
								for (var observation : observations) {
									try {
										// Add ecoregionId to message and publish
										OccurrenceMessage observationMessage = new OccurrenceMessage(
												message.ecoregionId(), message.regionName(), observation);
										if (observationMessage.occurrence().species() != null)
											publishObservation(observationMessage);
									} catch (IOException | TimeoutException e) {
										logger.error("Error while publishing {} to observations queue", observation, e);
										throw new IOException(e);
									}
								}
								// 2. Add messageHash to consumer db
								processedMessageCollection.insertOne(new Document("messageId", hash));
								// 3. Ack task queue to confirm message processing
								channel.basicAck(deliveryTag, false);

							} catch (InterruptedException | ExecutionException e) {
								channel.basicNack(deliveryTag, false, true);
								logger.error("Error while processing the message from queue {}", TASK_QUEUE_NAME, e);
							}
						} else {
							logger.info("Duplicate message for hash {}", hash);
							channel.basicAck(deliveryTag, false);
						}
					}

					private void publishObservation(OccurrenceMessage observation)
							throws IOException, TimeoutException {
						ConnectionFactory factory = new ConnectionFactory();
						factory.setHost(config.get("rabbitmq.exchange.host"));
						try (Connection connection = factory.newConnection();
								Channel channel = connection.createChannel()) {
							channel.exchangeDeclare(EXCHNAGE_NAME, BuiltinExchangeType.DIRECT, true);
							var message = new ObjectMapper().writeValueAsBytes(observation);
							channel.basicPublish(EXCHNAGE_NAME, ROUNTING_KEY, null, message);
							//logger.info(" [x] Sent for GBIF observation id '{}'", observation.observation().key());
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

	static record OccurrenceMessage(String ecoregionId, String regionName, Occurrence occurrence) {
	}
}
