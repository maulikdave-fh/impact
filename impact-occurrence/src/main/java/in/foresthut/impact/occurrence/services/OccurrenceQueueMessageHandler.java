package in.foresthut.impact.occurrence.services;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.occurrence.daos.OccurrenceInMessage;
import in.foresthut.impact.occurrence.repo.OccurrenceRepository;

public class OccurrenceQueueMessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(OccurrenceQueueMessageHandler.class);
	private static OccurrenceRepository occurrenceRepo;
	private final static Config config = Config.getInstance();
	private final static String OCCURRENCE_QUEUE_NAME = config.get("rabbitmq.occurrence.queue.name");
	private final static String EXCHANGE_NAME = config.get("rabbitmq.exchange.name");
	private final static String OCCURRENCE_FROM_FILE_ROUTING_KEY = "fromFile";
	private final static String OCCURRENCE_DAILY_RUN_ROUTING_KEY = "dailyRun";
	private final static int NUMBER_OF_CONSUMERS = 4;

	private OccurrenceQueueMessageHandler() {

	}

	public static void start() throws IOException, TimeoutException {
		occurrenceRepo = OccurrenceRepository.getInstance();

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.occurrence.host"));
		Connection connection = factory.newConnection();

		// Creating multiple consumers
		for (int i = 0; i < NUMBER_OF_CONSUMERS; i++) {
			createConsumers(connection, i + 1, occurrenceRepo);
		}
	}

	private static void createConsumers(Connection connection, int consumerId, OccurrenceRepository repo)
			throws IOException {
		// Create a channel for each consumer
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true);
		channel.basicQos(1);
		
		// Declare the queue as durable
		channel.queueDeclare(OCCURRENCE_QUEUE_NAME, true, false, false, null);
		
		// Bind the queue with exchange
		channel.queueBind(OCCURRENCE_QUEUE_NAME, EXCHANGE_NAME, OCCURRENCE_FROM_FILE_ROUTING_KEY);
		channel.queueBind(OCCURRENCE_QUEUE_NAME, EXCHANGE_NAME, OCCURRENCE_DAILY_RUN_ROUTING_KEY);

		// Create a consumer
		boolean autoAck = false;
		channel.basicConsume(OCCURRENCE_QUEUE_NAME, autoAck, String.valueOf("Consumer" + consumerId),
				new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
							byte[] body) throws IOException {
						long deliveryTag = envelope.getDeliveryTag();
						OccurrenceInMessage occurrenceInMsg = new ObjectMapper().readValue(body,
								OccurrenceInMessage.class);
						logger.info("Consumer {} received {}", consumerId, occurrenceInMsg);

						// Check for a duplicate observation in observation Db
						String regionName = occurrenceInMsg.regionName();
						String gbifOccurrenceId = String.valueOf(occurrenceInMsg.occurrence().key());

						if (!repo.exists(regionName, gbifOccurrenceId)) {
							// 1. Add obervation to db
							repo.addOccurrence(occurrenceInMsg);
							// 2. Ack observation queue to confirm message processing
							channel.basicAck(deliveryTag, false);
							logger.info("Inserted observation with GBIF Observation key {} in Observation db",
									gbifOccurrenceId);

						} else {
							logger.info("Duplicate message for GBIF Observation key {}", gbifOccurrenceId);
							// 1. Update observartion in db
							repo.updateOccurrence(occurrenceInMsg);
							// 2. Ack observation queue to confirm message processing
							channel.basicAck(deliveryTag, false);
							logger.info("Updated observation with GBIF Observation key {} in Observation db",
									gbifOccurrenceId);
						}
					}
				});

		logger.info("Consumer " + consumerId + " is waiting for messages.");
	}

}
