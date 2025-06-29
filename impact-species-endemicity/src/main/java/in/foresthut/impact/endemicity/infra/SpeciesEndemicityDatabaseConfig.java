package in.foresthut.impact.endemicity.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import in.foresthut.impact.config.Config;


public class SpeciesEndemicityDatabaseConfig {
	private static SpeciesEndemicityDatabaseConfig instance;
	private MongoDatabase speciesEndemicityDb;
	private static final Logger logger = LoggerFactory.getLogger(SpeciesEndemicityDatabaseConfig.class);
	private static Config config = Config.getInstance();

	private SpeciesEndemicityDatabaseConfig() {
		String connectionString = config.get("mongodb.uri");
		MongoClient mongoClient = MongoClients.create(connectionString);
		speciesEndemicityDb = mongoClient.getDatabase(config.get("mongodb.database"));
		logger.info("Connected to mongodb successfully.");
	}

	public static synchronized SpeciesEndemicityDatabaseConfig getInstance() {
		if (instance == null)
			instance = new SpeciesEndemicityDatabaseConfig();
		return instance;
	}
	
	public MongoDatabase database() {
		return speciesEndemicityDb;
	}
}