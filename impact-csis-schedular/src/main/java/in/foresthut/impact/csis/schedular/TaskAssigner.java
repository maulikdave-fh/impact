package in.foresthut.impact.csis.schedular;

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

import in.foresthut.impact.csis.schedular.infra.CSISTasksDBConfiguration;
import in.foresthut.impact.csis.schedular.infra.EcoregionClient;
import in.foresthut.impact.csis.schedular.infra.EcoregionClient.Ecoregion;
import in.foresthut.impact.csis.schedular.infra.OccurrenceClient;

public class TaskAssigner implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(TaskAssigner.class);
	private static MongoDatabase csisTaskDatabase = CSISTasksDBConfiguration.getInstance();
	private MongoCollection<Document> taskCollection;
	private EcoregionClient ecoregionClient;
	private OccurrenceClient occurrenceClient;

	public TaskAssigner() {
		taskCollection = csisTaskDatabase.getCollection("tasks");
		ecoregionClient = new EcoregionClient();
		occurrenceClient = OccurrenceClient.getInstance();
	}

	@Override
	public void run() {
		List<Ecoregion> ecoregions = new ArrayList<>();
		try {
			ecoregions = ecoregionClient.ecoregions();
		} catch (IOException | InterruptedException e) {
			logger.error("Error while fetching ecoregions' details", e);
		}

		for (var ecoregion : ecoregions) {
			long count = taskCollection.countDocuments(new BsonDocument("ecoregionId", new BsonString(ecoregion.id())));
			// if tasks don't exist for a given ecoregion
			if (count == 0) {
				try {
					List<String> splits = ecoregionClient.split(ecoregion.id());
					List<Document> dbTasks = new ArrayList<>();
					for (var split : splits) {
						String startDate = occurrenceClient.latestModifiedOn(ecoregion.region(), ecoregion.id());
						dbTasks.add(new Document("ecoregionId", ecoregion.id()).append("regionName", ecoregion.region())
								.append("polygon", split).append("dateFrom", startDate).append("outbox", true));
					}
					taskCollection.insertMany(dbTasks);
					logger.info("{} tasks created for ecoregionId {}", dbTasks.size(), ecoregion.id());
				} catch (IOException | InterruptedException | ExecutionException e) {
					logger.error("{}", e);
				}
			} else {
				Bson ecoregionIdFilter = Filters.eq("ecoregionId", ecoregion.id());
				Bson outboxTaskFilter = Filters.eq("outbox", false);
				Bson combinedFilter = Filters.and(ecoregionIdFilter, outboxTaskFilter);

				String newDateFrom = LocalDate.now().minusDays(1).toString();
				Bson dateFromUpdate = Updates.set("dateFrom", newDateFrom);
				Bson outboxUpdate = Updates.set("outbox", true);
				UpdateOptions updateOptions = new UpdateOptions().upsert(false);

				Bson combinedUpdates = Updates.combine(dateFromUpdate, outboxUpdate);

				UpdateResult updateResult = taskCollection.updateMany(combinedFilter, combinedUpdates, updateOptions);
				logger.info("Tasks updated for ecoregionId {} - {}", ecoregion.id(), updateResult);
			}
		}
	}
}
