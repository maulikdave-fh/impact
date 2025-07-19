package in.foresthut.impact.habitat.finder.repo;

import java.util.ArrayList;
import java.util.List;
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
import com.mongodb.client.model.Filters;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.config.Config;
import in.foresthut.impact.habitat.finder.daos.IUCNHabitat;

public class IUCNHabitatsRepo {
	private static IUCNHabitatsRepo instance;
	private MongoClient mongoClient;
	private MongoDatabase habitatDb;
	private static final Logger logger = LoggerFactory.getLogger(IUCNHabitatsRepo.class);
	private static Config config = Config.getInstance();

	private IUCNHabitatsRepo() {
		String connectionString = config.get("mongodb.uri");
		logger.info("Trying to connect to mongo db at {}", connectionString);

		// Construct a ServerApi instance using the ServerApi.builder() method
		ServerApi serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();

		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(connectionString)).serverApi(serverApi).build();

		// Create a new client and connect to the server
		mongoClient = MongoClients.create(settings);
		habitatDb = mongoClient.getDatabase(config.get("mongodb.database"));
		try {
			// Send a ping to confirm a successful connection
			Bson command = new BsonDocument("ping", new BsonInt64(1));
			Document commandResult = habitatDb.runCommand(command);
			logger.info("Connected successfully to mongo db at {} with ping results {}", connectionString,
					commandResult);
		} catch (MongoException me) {
			final String traceId = UUID.randomUUID().toString();
			logger.error("[Error-trace-id] {} Error while connecting to mongo db at {}", traceId, connectionString, me);
			throw new AlreadyLoggedException(
					String.format("Error while connecting to mongo db at %s", connectionString), traceId, me);
		}
	}

	public static synchronized IUCNHabitatsRepo getInstance() {
		if (instance == null)
			instance = new IUCNHabitatsRepo();
		return instance;
	}

	public MongoDatabase database() {
		return habitatDb;
	}

	public void add(IUCNHabitat habitat) {
		var collection = habitatDb.getCollection("iucn_habitats");
		collection.insertOne(new Document("key", habitat.key()).append("name", habitat.name())
				.append("keywords", habitat.keywords()).append("lowerElevation", habitat.lowerElevation())
				.append("higherElevation", habitat.upperElevation()));
		logger.info("Added {}", habitat);
	}

	public List<IUCNHabitat> getAll() {
		var collection = habitatDb.getCollection("iucn_habitats");
		var habitats = collection.find();
		List<IUCNHabitat> result = new ArrayList<>();
		for (var habitat : habitats) {
			result.add(new IUCNHabitat(habitat.getDouble("key"), habitat.getString("name"),
					habitat.getList("keywords", String.class), habitat.getInteger("lowerElevation"),
					habitat.getInteger("upperElevation")));
		}
		return result;
	}

	public IUCNHabitat get(double key) {
		var collection = habitatDb.getCollection("iucn_habitats");
		Bson filter = Filters.eq("key", key);
		var habitat = collection.find(filter).first();

		IUCNHabitat result = null;
		if (habitat != null) {
			result = new IUCNHabitat(habitat.getDouble("key"), habitat.getString("name"),
					habitat.getList("keywords", String.class), habitat.getInteger("lowerElevation"),
					habitat.getInteger("upperElevation"));
		}
		return result;
	}
}