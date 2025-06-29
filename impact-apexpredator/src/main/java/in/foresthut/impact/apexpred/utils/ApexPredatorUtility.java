package in.foresthut.impact.apexpred.utils;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;

import in.foresthut.impact.apexpred.infa.ApexPredatorDBConfiguration;
import in.foresthut.impact.commons.filters.BloomFilter;

public class ApexPredatorUtility {
	private static ApexPredatorUtility instance;
	private static final Logger logger = LoggerFactory.getLogger(ApexPredatorUtility.class);

	private final BloomFilter bloomFilter;

	private ApexPredatorUtility() {
		// number of elements = 25, probablity of false positives = 0.0001 (1 in 10000)
		// nummber of bits (size of bloom filter) = 1579 (197B), number of hash functions = 3
		this.bloomFilter = new BloomFilter(1600);

		var trophicWebDB = ApexPredatorDBConfiguration.getInstance().database();
		var apexPredatorCollection = trophicWebDB.getCollection("apex_predators");
		var apexPredators = apexPredatorCollection.find();

		for (var apexPredator : apexPredators)
			this.bloomFilter.add(apexPredator.getString("name"));
	}

	public static ApexPredatorUtility getInstance() {
		if (instance == null)
			instance = new ApexPredatorUtility();
		return instance;
	}

	public boolean isApexPredator(String species) {
		if (bloomFilter.mightContain(species)) {
			logger.info("{} found in the BloomFilter. Going to DB for further check.", species);
			return isApexPredatorInDB(species);
		} else {
			logger.info("{} didn't find in the BloomFilter. It is not an apex predator", species);
			return false;
		}
	}

	private boolean isApexPredatorInDB(String species) {
		var trophicWebDB = ApexPredatorDBConfiguration.getInstance().database();
		var apexPredatorCollection = trophicWebDB.getCollection("apex_predators");

		Bson filter = Filters.eq("name", species);
		long recordCount = apexPredatorCollection.countDocuments(filter);
		if (recordCount > 0)
			logger.info("{} found in DB. It is an apex predator.", species);
		else
			logger.info("{} not found in DB. It is not an apex predator.", species);
		return recordCount > 0;
	}
}
