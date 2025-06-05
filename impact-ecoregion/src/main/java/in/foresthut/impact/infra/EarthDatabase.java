package in.foresthut.impact.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import in.foresthut.impact.Config;


public class EarthDatabase {
	private static EarthDatabase instance;
	private MongoDatabase earthDatabase;
	private static final Logger logger = LoggerFactory.getLogger(EarthDatabase.class);
	private static Config config = Config.getInstance();

	private EarthDatabase() {
		String connectionString = config.get("mongodb.uri");
		MongoClient mongoClient = MongoClients.create(connectionString);
		earthDatabase = mongoClient.getDatabase(config.get("mongodb.database"));
		logger.info("Connected to mongodb successfully.");
	}

	public static synchronized MongoDatabase getInstance() {
		if (instance == null)
			instance = new EarthDatabase();
		return instance.earthDatabase;
	}
}