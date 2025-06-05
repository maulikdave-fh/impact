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
	}
}
