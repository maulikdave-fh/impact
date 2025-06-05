package in.foresthut.impact;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import in.foresthut.impact.infra.CSISTasksDBConfiguration;

public class TaskPublisher implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(TaskPublisher.class);
	private static MongoDatabase csisTaskDatabase = CSISTasksDBConfiguration.getInstance();
	private final static Config config = Config.getInstance();
	private final static String QUEUE_NAME = config.get("rabbitmq.queue.name");
	private MongoCollection<Document> taskCollection;

	public TaskPublisher() {
		taskCollection = csisTaskDatabase.getCollection("tasks");
	}

	@Override
	public void run() {
		// 1. Fetch all the records with outbox = true
		Bson outboxTaskFilter = Filters.eq("outbox", true);
		var dbTasks = taskCollection.find(outboxTaskFilter);

		// 2. Loop throuh the records
		for (var dbTask : dbTasks) {
			String ecoregionId = dbTask.getString("ecoregionId");
			String polygon = dbTask.getString("polygon");
			String dateFrom = dbTask.getString("dateFrom");
			// 2a. Calculate hash
			int hash = hash(ecoregionId, polygon, dateFrom);
			// 2b. Set a task with dateTo = yesterday's date
			String dateTo = LocalDate.now().minusDays(1).toString();
			Task task = new Task(ecoregionId, polygon, dateFrom, dateTo, hash);
			// 2c. Publish task to queue
			try {
				publish(task);
				// 2d. Set outbox = false in database
				ObjectId _id = dbTask.getObjectId("_id");
				Bson idFilter = Filters.eq("_id", _id);
				Bson outboxUpdate = Updates.set("outbox", false);
				UpdateOptions updateOptions = new UpdateOptions().upsert(false);
				
				UpdateResult updateResult = taskCollection.updateOne(idFilter, outboxUpdate, updateOptions);
				logger.info("Task updated for ecoregionId {} - {}", ecoregionId, updateResult);

			} catch (IOException | TimeoutException e) {
				logger.error("Error while publishing message with ecoregionId {} to queue", ecoregionId, e);
			}
		}
	}

	private void publish(Task task) throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.host"));
		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null);
			var message = new ObjectMapper().writeValueAsBytes(task);
			channel.basicPublish("", QUEUE_NAME, null, message);
			logger.info(" [x] Sent '{}'", task);
		} catch (IOException e) {
			logger.error("{}", e);
			throw e;
		} catch (TimeoutException e1) {
			logger.error("{}", e1);
			throw e1;
		}
	}

	private int hash(String ecoreigonId, String polygon, String dateFrom) {
		return new StringBuilder(ecoreigonId).append(polygon).append(dateFrom).hashCode();
	}

	private static record Task(String ecoregionId, String polygon, String dateFrom, String dateTo, int messageHash) {
	}
}
