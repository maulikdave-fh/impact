package in.foresthut.impact;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import in.foresthut.impact.ecoregion.EcoregionClient;
import in.foresthut.impact.infra.CSISTasksDBConfiguration;

public class TaskAssigner implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(TaskAssigner.class);
	private static MongoDatabase csisTaskDatabase = CSISTasksDBConfiguration.getInstance();
	private final static Config config = Config.getInstance();
	private final static String QUEUE_NAME = config.get("rabbitmq.queue.name");
	private MongoCollection<Document> taskCollection;
	private EcoregionClient ecoregionClient;

	private static final String PRIMARY_START_DATE = "1500-01-01";

	public TaskAssigner() {
		taskCollection = csisTaskDatabase.getCollection("tasks");
		ecoregionClient = new EcoregionClient();
	}

	@Override
	public void run() {
		List<String> ecoregions = new ArrayList<>();
		try {
			ecoregions = ecoregionClient.ecoregions();
		} catch (IOException | InterruptedException e) {
			logger.error("Error while fetching ecoregions' details", e);
		}

		for (var ecoregionId : ecoregions) {
			long count = taskCollection.countDocuments(new BsonDocument("ecoregionId", new BsonString(ecoregionId)));
			// if tasks don't exist for a given ecoregion
			if (count == 0) {
				try {
					List<String> splits = ecoregionClient.split(ecoregionId);
					List<Document> dbTasks = new ArrayList<>();
					for (var split : splits) {
						dbTasks.add(new Document("ecoregionId", ecoregionId).append("polygon", split)
								.append("dateFrom", PRIMARY_START_DATE).append("outbox", true));
					}
					taskCollection.insertMany(dbTasks);
					logger.info("{} tasks created for ecoregionId {}", dbTasks.size(), ecoregionId);
				} catch (IOException | InterruptedException | ExecutionException e) {
					logger.error("{}", e);
				}
			} else {
				Bson ecoregionIdFilter = Filters.eq("ecoregionId", ecoregionId);
				Bson outboxTaskFilter = Filters.eq("outbox", false);
				Bson combinedFilter = Filters.and(ecoregionIdFilter, outboxTaskFilter);

				String newDateFrom = LocalDate.now().minusDays(1).toString();
				Bson dateFromUpdate = Updates.set("dateFrom", newDateFrom);
				Bson outboxUpdate = Updates.set("outbox", true);
				UpdateOptions updateOptions = new UpdateOptions().upsert(false);

				Bson combinedUpdates = Updates.combine(dateFromUpdate, outboxUpdate);

				UpdateResult updateResult = taskCollection.updateMany(combinedFilter, combinedUpdates, updateOptions);
				logger.info("Tasks updated for ecoregionId {} - {}", ecoregionId, updateResult);
			}
		}

//		ConnectionFactory factory = new ConnectionFactory();
//		factory.setHost(config.get("rabbitmq.host"));
//		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
//			channel.queueDeclare(QUEUE_NAME, false, false, false, null);
//			for (var ecoregion : ecoregions.entrySet()) {
//				for (var polygon : ecoregion.getValue()) {
//					var task = new Task(ecoregion.getKey(), lastFetchedFor.plusMonths(1).toString(), polygon);
//					var message = new ObjectMapper().writeValueAsBytes(task);
//					channel.basicPublish("", QUEUE_NAME, null, message);
//					logger.info(" [x] Sent '{}'", task);
//					// TODO: Remove the break
//					break;
//				}
//				// TODO: Remove the break
//				break;
//			}
//		} catch (IOException e) {
//			logger.error("{}", e);
//		} catch (TimeoutException e1) {
//			logger.error("{}", e1);
//		}
//
//		Bson filter = Filters.eq("_id", gbifConfig.getObjectId("_id"));
//		Bson update = null;
//		if (isMonthlyFetch) {
//			update = Updates.set("lastFetchedFor", lastFetchedFor.plusMonths(1).toString());
//			taskCollection.updateOne(filter, update);
//		} else {
//			taskCollection.updateMany(filter, Updates.combine(
//					Updates.set("lastFetchedFor", today.minusDays(1).toString()), Updates.set("frequencyMins", 1440)));
//		}
	}

}
