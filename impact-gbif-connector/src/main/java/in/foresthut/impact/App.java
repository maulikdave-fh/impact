package in.foresthut.impact;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import in.foresthut.impact.gbif.GBIFClient;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);

	private final static Config config = Config.getInstance();
	private final static String QUEUE_NAME = config.get("rabbitmq.queue.name");

	public static void main(String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.host"));
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		logger.info(" [*] Waiting for messages. To exit press CTRL+C");

		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			Task message = new ObjectMapper().readValue(delivery.getBody(), Task.class);
			logger.info(" [x] Received '" + message + "'");
			try {
				logger.info("Received from GBIF {}", new GBIFClient().observations(message));
			} catch (InterruptedException | ExecutionException e) {
				logger.error("Error while received the message from queue {}", QUEUE_NAME, e);
			}
		};
		channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
		});
	}
}
