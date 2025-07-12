package in.foresthut.impact.species.attributes;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.species.attributes.utils.IntroducedUtility;
import in.foresthut.impact.species.attributes.utils.InvasiveUtility;
import in.foresthut.impact.species.attributes.utils.KeySpeciesUtility;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private final static Config config = Config.getInstance();
	private final static String SOURCE_QUEUE_NAME = config.get("rabbitmq.source.queue.name");
	private final static String TARGET_QUEUE_NAME = config.get("rabbitmq.target.queue.name");
	private final static int NUMBER_OF_CONSUMERS = 4;

	public static void main(String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.source.host"));
		Connection connection = factory.newConnection();

		// Creating multiple consumers
		for (int i = 0; i < NUMBER_OF_CONSUMERS; i++) {
			createConsumers(connection, i + 1);
		}
	}

	private static void createConsumers(Connection connection, int consumerId) throws IOException {
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
						try {
							long deliveryTag = envelope.getDeliveryTag();
							InMessage inMessage = new ObjectMapper().readValue(body, InMessage.class);
							logger.info("Consumer {} received {}", consumerId, inMessage);

							var regionName = inMessage.region();
							boolean keySpecies = KeySpeciesUtility.getInstance().isKeySpecies(inMessage.species(),
									regionName);

							boolean introduced = inMessage.endemicity() > 0.8 ? false
									: IntroducedUtility.getInstance().isIntroduced(inMessage.species(), regionName);
							boolean invasive = inMessage.endemicity() > 0.8 ? false
									: InvasiveUtility.getInstance().isInvasive(inMessage.species(), regionName);
							;

							OutMessage outMsg = toOutMessage(inMessage, keySpecies, introduced, invasive);
							publishOutMessage(outMsg);

							channel.basicAck(deliveryTag, false);
						} catch (Exception ex) {
							logger.error("Error while processing the message from queue {}", SOURCE_QUEUE_NAME, ex);
							channel.basicNack(consumerId, false, true);
						}
					}

					private void publishOutMessage(OutMessage outMessage) throws IOException, TimeoutException {
						ConnectionFactory factory = new ConnectionFactory();
						factory.setHost(config.get("rabbitmq.target.host"));
						try (Connection connection = factory.newConnection();
								Channel channel = connection.createChannel()) {
							channel.queueDeclare(TARGET_QUEUE_NAME, false, false, false, null);
							var message = new ObjectMapper().writeValueAsBytes(outMessage);
							channel.basicPublish("", TARGET_QUEUE_NAME, null, message);
							logger.info(" [x] Sent '{}'", outMessage);
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

	static OutMessage toOutMessage(InMessage inMessage, boolean keySpecies, boolean introduced, boolean invasive) {
		return new OutMessage(inMessage.ecoregionId(), inMessage.region(), inMessage.species(), inMessage.kingdom(),
				inMessage.phylum(), inMessage._class(), inMessage.order(), inMessage.family(),
				inMessage.iucnRedListCategory(), inMessage.apexPredator(), inMessage.endemicity(), keySpecies,
				introduced, invasive);
	}

	static record OutMessage(String ecoregionId, String region, String species, String kingdom, String phylum,
			String _class, String order, String family, String iucnRedListCategory, boolean apexPredator,
			double endemicity, boolean keySpecies, boolean introduced, boolean invasive) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static record InMessage(String ecoregionId, String region, String species, String kingdom, String phylum,
			String _class, String order, String family, String iucnRedListCategory, boolean apexPredator,
			double endemicity) {
	}

}
