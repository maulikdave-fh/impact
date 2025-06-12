package in.foresthut.impact.gbif.connector.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import in.foresthut.impact.config.Config;


public class CSISProcessedMessagesDBConfiguration {
	private static CSISProcessedMessagesDBConfiguration instance;
	private MongoDatabase csisTasksDBConfiguration;
	private static final Logger logger = LoggerFactory.getLogger(CSISProcessedMessagesDBConfiguration.class);
	private static Config config = Config.getInstance();

	private CSISProcessedMessagesDBConfiguration() {
		String connectionString = config.get("mongodb.uri");
		MongoClient mongoClient = MongoClients.create(connectionString);
		csisTasksDBConfiguration = mongoClient.getDatabase(config.get("mongodb.database"));
		logger.info("Connected to mongodb successfully.");
	}

	public static synchronized MongoDatabase getInstance() {
		if (instance == null)
			instance = new CSISProcessedMessagesDBConfiguration();
		return instance.csisTasksDBConfiguration;
	}
}