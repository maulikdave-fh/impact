package in.foresthut.impact.occurrence.repo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import com.mongodb.client.model.Sorts;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.commons.filters.BloomFilter;
import in.foresthut.impact.config.Config;
import in.foresthut.impact.occurrence.daos.Occurrence;
import in.foresthut.impact.occurrence.daos.OccurrenceInMessage;
import in.foresthut.impact.utils.datetime.DateTimeUtil;

public class OccurrenceRepository {
	private static OccurrenceRepository instance;
	private MongoDatabase occurrenceDatabase;
	private static final Logger logger = LoggerFactory.getLogger(OccurrenceRepository.class);
	private static Config config = Config.getInstance();
	private static BloomFilter bloomFilter = null;

	private OccurrenceRepository() {
		// number of elements = 400000, probablity of false positives = 0.0000001 (1 in
		// 10000000)
		// nummber of bits (257931698 (30.75MiB)), number of hash functions = 3
		bloomFilter = new BloomFilter(257931698);

		String connectionString = config.get("mongodb.uri");
		logger.info("Trying to connect to mongo db at {}", connectionString);

		// Construct a ServerApi instance using the ServerApi.builder() method
		ServerApi serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();

		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(connectionString)).serverApi(serverApi).build();

		// Create a new client and connect to the server
		MongoClient mongoClient = MongoClients.create(settings);
		occurrenceDatabase = mongoClient.getDatabase(config.get("mongodb.database"));
		try {
			// Send a ping to confirm a successful connection
			Bson command = new BsonDocument("ping", new BsonInt64(1));
			Document commandResult = occurrenceDatabase.runCommand(command);
			logger.info("Connected successfully to mongo db at {} with ping results {}", connectionString,
					commandResult);
		} catch (MongoException me) {
			final String traceId = UUID.randomUUID().toString();
			logger.error("[Error-trace-id] {} Error while connecting to mongo db at {}", traceId, connectionString, me);
			throw new AlreadyLoggedException(
					String.format("Error while connecting to mongo db at %s", connectionString), traceId, me);
		}
	}

	public static synchronized OccurrenceRepository getInstance() {
		if (instance == null)
			instance = new OccurrenceRepository();
		return instance;
	}

	public MongoDatabase getDatabase() {
		return this.occurrenceDatabase;
	}

	public void addOccurrence(OccurrenceInMessage occurrenceMessage) {
		var collection = occurrenceDatabase
				.getCollection(occurrenceMessage.regionName().replaceAll("\\s", "_").toLowerCase());
		collection.insertOne(toDocument(occurrenceMessage.ecoregionId(), occurrenceMessage.occurrence()));
	}

	public void updateOccurrence(OccurrenceInMessage occurrenceMessage) {
		Bson filter = Filters.eq("gbifKey", occurrenceMessage.occurrence().key());
		Document newObservation = OccurrenceRepository.toDocument(occurrenceMessage.ecoregionId(),
				occurrenceMessage.occurrence());

		var collection = occurrenceDatabase
				.getCollection(occurrenceMessage.regionName().replaceAll("\\s", "_").toLowerCase());
		collection.findOneAndReplace(filter, newObservation);
	}

	public boolean exists(String regionName, String key) {
		if (bloomFilter.mightContain(key)) {
			return false;
		} else {
			var collection = occurrenceDatabase.getCollection(regionName.replaceAll("\\s", "_").toLowerCase());
			Bson filter = Filters.eq("gbifKey", new BsonInt64(Long.valueOf(key)));
			long count = collection.countDocuments(filter);
			return count == 0 ? false : true;
		}
	}

	public Date latestModifiedOn(String ecoregionId, String regionName) {
		Bson filter = Filters.eq("ecoregionId", ecoregionId);
		var collection = occurrenceDatabase.getCollection(regionName.replaceAll("\\s", "_").toLowerCase());
		Bson sorter = Sorts.descending("modifiedOn");
		var result = collection.find(filter).sort(sorter).limit(1);

		Date latestModifiedOn = null;
		for (var item : result)
			latestModifiedOn = item.getDate("modifiedOn");

		return latestModifiedOn;
	}

	public static Document toDocument(String ecoregionId, Occurrence occurrence) {
		Document geoJsonPoint = new Document("type", "Point").append("coordinates",
				List.of(occurrence.decimalLongitude(), occurrence.decimalLatitude()));
		String iconicTaxa = getIconicTaxa(occurrence);
		return new Document("gbifKey", occurrence.key()).append("species", occurrence.species())
				.append("kingdom", occurrence.kingdom()).append("phylum", occurrence.phylum())
				.append("_class", occurrence._class()).append("order", occurrence.order())
				.append("subFamily", occurrence.subFamily()).append("family", occurrence.family())
				.append("iconicTaxa", iconicTaxa).append("superFamily", occurrence.superFamily())
				.append("tribe", occurrence.tribe()).append("subTribe", occurrence.subTribe())
				.append("genus", occurrence.genus()).append("subGenus", occurrence.subGenus())
				.append("iucnStatus", occurrence.iucnRedListCategory()).append("location", geoJsonPoint)
				.append("eventDate", DateTimeUtil.parse(occurrence.eventDate()))
				.append("modifiedOn", DateTimeUtil.parse(occurrence.modified()))
				.append("csisName", occurrence.datasetName()).append("recordedBy", occurrence.recordedBy())
				.append("ecoregionId", ecoregionId);
	}

	private static String getIconicTaxa(Occurrence occurrence) {
		List<String> classes = new ArrayList<String>(
				Arrays.asList("Aves", "Mammalia", "Amphibia", "Squamata", "Insecta", "Arachnida"));
		List<String> actinopterygiiOrders = new ArrayList<String>(Arrays.asList("Acanthuriformes", "Acipenseriformes",
				"Alepocephaliformes", "Argentiniformes", "Ateleopodiformes", "Aulopiformes", "Centrarchiformes",
				"Clupeiformes", "Galaxiiformes", "Labriformes", "Lepidogalaxiiformes", "Myctophiformes", "Osmeriformes",
				"Perciformes", "Polypteriformes", "Salmoniformes", "Stomiiformes", "Amiiformes", "Lepisosteiformes",
				"Albuliformes", "Anguilliformes", "Elopiformes", "Notacanthiformes", "Characiformes", "Cypriniformes",
				"Gonorynchiformes", "Gymnotiformes", "Siluriformes", "Hiodontiformes", "Osteoglossiformes"));

		if (occurrence.kingdom() != null
				&& (occurrence.kingdom().equals("Plantae") || occurrence.kingdom().equals("Fungi"))) {
			return occurrence.kingdom();
		} else if (occurrence.kingdom() != null && occurrence.kingdom().equals("Animalia")) {
			if (occurrence.phylum() != null && occurrence.phylum().equals("Mollusca")) {
				return occurrence.phylum();
			} else if (occurrence._class() != null && classes.contains(occurrence._class())) {
				return occurrence._class();
			} else if (occurrence.order() != null && actinopterygiiOrders.contains(occurrence.order())) {
				return "Actinopterygii";
			}
		}
		return "Other";
	}
}