package in.foresthut.impact.species.eni;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import in.foresthut.impact.config.Config;
import in.foresthut.impact.species.eni.infra.GeminiClient;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private final static Config config = Config.getInstance();
	private final static StanfordCoreNLP nlpSSPipeline = NlpSentenceSplitterPipeline.getInstance();
	private final static NlpSentimentPipeline nlpSentimentPipeline = NlpSentimentPipeline.getInstance();
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
						var inMsg = new ObjectMapper().readValue(body, InMessage.class);
						logger.info("Consumer {} received {}", consumerId, inMsg);

						try {
							String[] prompts = new String[] {
									"Is " + inMsg.speciesName() + " endemic to the " + inMsg.region() + "?",
									"Is " + inMsg.speciesName() + " native to the " + inMsg.region() + "?",
									"Is " + inMsg.speciesName() + " invasive in the " + inMsg.region() + "?" };
							String response = geminiClient.send(prompts);

							List<List<String>> sentences = getSentences(response, inMsg.region());
							logger.info(consumerTag);

							
							boolean endemic = false;
							if (!sentences.get(0).isEmpty()) {
								endemic = nlpSentimentPipeline.estimateSentiment(sentences.get(0).get(0)) == 1 ? false
										: true;
							}
							logger.info("Endemic flag for {} is {}", inMsg.speciesName(), endemic);

							
							
							boolean isNative = endemic;
							if (!isNative && !sentences.get(1).isEmpty()) {
								isNative = nlpSentimentPipeline.estimateSentiment(sentences.get(1).get(0)) == 1 ? false
										: true;
							}
							logger.info("Native flag for {} is {}", inMsg.speciesName(), isNative);

					
							boolean invasive = false;
							if (!sentences.get(2).isEmpty()) {
								invasive = nlpSentimentPipeline.estimateSentiment(sentences.get(2).get(0)) == 1 ? true
										: false;
							}
							logger.info("Invasive flag for {} is {}", inMsg.speciesName(), invasive);
							
							
							OutMessage outMsg = new OutMessage(inMsg.speciesName(), inMsg.region(), endemic, isNative,
									invasive);

							publishMessage(channel, deliveryTag, outMsg);

						} catch (Exception e) {
							channel.basicNack(deliveryTag, false, true);
							logger.error("Error while processing the message from queue {}", SOURCE_QUEUE_NAME, e);
						}

					}

					private List<List<String>> getSentences(String response, String region) {
						CoreDocument doc = new CoreDocument(response);
						nlpSSPipeline.annotate(doc);

						List<String> endemicSentences = new ArrayList<>();
						List<String> nativeSentences = new ArrayList<>();
						List<String> invasiveSentences = new ArrayList<>();

						for (var sentence : doc.sentences()) {
							var str = sentence.toString();
							var furtherSplits = str.split(",|:");
							logger.info("Further splits {}", Arrays.toString(furtherSplits));
							for (var splitSent : furtherSplits) {
								if (!str.contains("?")) {
									splitSent = splitSent.replaceAll("\\*", "").trim();
									if (splitSent.contains(region) || splitSent.contains("this region")) {
										if (splitSent.contains("endemic"))
											endemicSentences.add(splitSent);
									}
									if (splitSent.contains(region) || splitSent.contains("this region") || splitSent.contains("India")) {
										if (splitSent.contains("native"))
											nativeSentences.add(splitSent);
										else if (splitSent.contains("invasive"))
											invasiveSentences.add(splitSent);
									}
								}
							}
						}

						return new ArrayList<List<String>>(
								List.of(endemicSentences, nativeSentences, invasiveSentences));
					}

					private void publishMessage(Channel channel, long deliveryTag, OutMessage outMsg)
							throws IOException {
						try {
							publishMessage(outMsg);
							channel.basicAck(deliveryTag, false);
						} catch (IOException | TimeoutException e) {
							channel.basicNack(deliveryTag, false, true);
							logger.error("Error while publishing a message {} to {}", outMsg, TARGET_QUEUE_NAME, e);
						}
					}

					private void publishMessage(OutMessage msg) throws IOException, TimeoutException {
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

	static record InMessage(String speciesName, String region) {
	}

	static record OutMessage(String speciesName, String region, boolean endemic, boolean isNative, boolean invasive) {
	}

}
