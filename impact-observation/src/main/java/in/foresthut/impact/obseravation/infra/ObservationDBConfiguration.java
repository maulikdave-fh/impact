package in.foresthut.impact.obseravation.infra;

import java.util.UUID;

import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import in.foresthut.impact.commons.AlreadyLoggedException;
import in.foresthut.impact.config.Config;


public class ObservationDBConfiguration {
	private static ObservationDBConfiguration instance;
	private MongoDatabase observationDatabase;
	private static final Logger logger = LoggerFactory.getLogger(ObservationDBConfiguration.class);
	private static Config config = Config.getInstance();

	private ObservationDBConfiguration() {
		String connectionString = config.get("mongodb.uri");
		logger.info("Trying to connect to mongo db at {}", connectionString);

		// Construct a ServerApi instance using the ServerApi.builder() method
		ServerApi serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();

		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(connectionString)).serverApi(serverApi).build();

		// Create a new client and connect to the server
		MongoClient mongoClient = MongoClients.create(settings);
		observationDatabase = mongoClient.getDatabase(config.get("mongodb.database"));
		try {
			// Send a ping to confirm a successful connection
			Bson command = new BsonDocument("ping", new BsonInt64(1));
			Document commandResult = observationDatabase.runCommand(command);
			logger.info("Connected successfully to mongo db at {} with ping results {}", connectionString,
					commandResult);
		} catch (MongoException me) {
			final String traceId = UUID.randomUUID().toString();
			logger.error("[Error-trace-id] {} Error while connecting to mongo db at {}", traceId, connectionString, me);
			throw new AlreadyLoggedException(
					String.format("Error while connecting to mongo db at %s", connectionString), traceId, me);
		}
	}

	public static synchronized MongoDatabase getInstance() {
		if (instance == null)
			instance = new ObservationDBConfiguration();
		return instance.observationDatabase;
	}
}