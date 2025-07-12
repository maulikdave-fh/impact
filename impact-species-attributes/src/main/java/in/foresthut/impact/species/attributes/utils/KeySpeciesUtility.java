package in.foresthut.impact.species.attributes.utils;

import java.util.HashMap;
import java.util.Map;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import in.foresthut.impact.commons.filters.BloomFilter;
import in.foresthut.impact.species.attributes.infa.SpeciesAttributesDBConfiguration;

public class KeySpeciesUtility {
	private static KeySpeciesUtility instance;
	private static final Logger logger = LoggerFactory.getLogger(KeySpeciesUtility.class);

	private final static Map<String, BloomFilter> bloomFilters = new HashMap<>();
	private final static MongoDatabase speciesAttributesDB = SpeciesAttributesDBConfiguration.getInstance().database();

	private KeySpeciesUtility() {

	}

	public static KeySpeciesUtility getInstance() {
		if (instance == null)
			instance = new KeySpeciesUtility();
		return instance;
	}

	public boolean isKeySpecies(String species, String region) {
		if (bloomFilters.containsKey(region)) {
			logger.info("Bloom filter found for region {}", region);
			var bloomFilter = bloomFilters.get(region);
			if (bloomFilter.mightContain(species)) {
				logger.info("{} found in the BloomFilter. Going to DB for further check.", species);
				return isKeySpeciesInDB(species, region);
			} else {
				logger.info("{} didn't find in the BloomFilter. It is not a key species", species);
				return false;
			}
		} else {
			// number of elements = 40, probablity of false positives = 0.0000001 (1 in
			// 10000000)
			// nummber of bits (size of bloom filter) = 25794 (3.15KiB), number of hash
			// functions = 3
			BloomFilter bloomFilter = new BloomFilter(25794);

			var keySpeciesCollection = speciesAttributesDB.getCollection("key_species");
			var keySpecies = keySpeciesCollection.find();

			boolean isKeySpecies = false;
			for (var kSpecies : keySpecies) {
				var speciesName = kSpecies.getString("name");
				bloomFilter.add(speciesName);
				if (speciesName.equals(species))
					isKeySpecies = true;
			}
			
			bloomFilters.put(region, bloomFilter);
			return isKeySpecies;
		}
	}

	private boolean isKeySpeciesInDB(String species, String region) {
		var keySpeciesCollection = speciesAttributesDB.getCollection("key_species");

		Bson filter = Filters.eq("name", species);
		long recordCount = keySpeciesCollection.countDocuments(filter);
		if (recordCount > 0)
			logger.info("{} found in DB. It is a key species.", species);
		else
			logger.info("{} not found in DB. It is not a key species.", species);
		return recordCount > 0;
	}
}
