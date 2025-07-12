package in.foresthut.impact.apexpred;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import in.foresthut.impact.apexpred.infa.EcoregionClient;
import in.foresthut.impact.apexpred.utils.ApexPredatorUtility;
import in.foresthut.impact.config.Config;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private final static Config config = Config.getInstance();
	private final static String SOURCE_QUEUE_NAME = config.get("rabbitmq.source.queue.name");
	private final static String TARGET_QUEUE_NAME = config.get("rabbitmq.target.queue.name");
	private final static String EXCHANGE_NAME = config.get("rabbitmq.exchange.name");
	private final static int NUMBER_OF_CONSUMERS = 4;
	private static Map<String, String> ecoRegionToRegionName = new HashMap<>();


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
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
		channel.basicQos(1);

		// Bind the queue with the exchange
		channel.queueBind(SOURCE_QUEUE_NAME, EXCHANGE_NAME, "");

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

							boolean apexPredator = ApexPredatorUtility.getInstance()
									.isApexPredator(inMessage.observation().species());
							var regionName = region(inMessage.ecoregionId());
							OutMessage outMsg = toOutMessage(inMessage.ecoregionId(), regionName, inMessage.observation(),
									apexPredator);
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

	private static String region(String ecoregionId) {
		if (ecoRegionToRegionName.isEmpty())
			ecoRegionToRegionName = new EcoregionClient().ecoregions();
		return ecoRegionToRegionName.get(ecoregionId);
	}
	
	static OutMessage toOutMessage(String ecoregionId, String region, Observation observation, boolean apexPredator) {
		return new OutMessage(ecoregionId, region, observation.species(), observation.kingdom(), observation.phylum(),
				observation._class(), observation.order(), observation.family(), observation.iucnRedListCategory(),
				apexPredator);
	}

	static record OutMessage(String ecoregionId, String region, String species, String kingdom, String phylum,
			String _class, String order, String family, String iucnRedListCategory,
			boolean apexPredator) {
	}

	static record InMessage(String ecoregionId, Observation observation) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static record Observation(long key, String species, String kingdom, String phylum,
			@JsonProperty(value = "class") String _class, String order, String family, String iucnRedListCategory,
			double decimalLatitude, double decimalLongitude, String eventDate, String modified, String datasetName,
			String recordedBy) {
	}

}
