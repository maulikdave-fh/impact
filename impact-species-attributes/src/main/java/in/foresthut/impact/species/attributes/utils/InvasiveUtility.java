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

public class InvasiveUtility {
	private static InvasiveUtility instance;
	private static final Logger logger = LoggerFactory.getLogger(InvasiveUtility.class);

	private final static Map<String, BloomFilter> bloomFilters = new HashMap<>();
	private final static MongoDatabase speciesAttributesDB = SpeciesAttributesDBConfiguration.getInstance().database();

	private InvasiveUtility() {

	}

	public static InvasiveUtility getInstance() {
		if (instance == null)
			instance = new InvasiveUtility();
		return instance;
	}

	public boolean isInvasive(String species, String region) {
		if (bloomFilters.containsKey(region)) {
			logger.info("Bloom filter found for region {}", region);
			var bloomFilter = bloomFilters.get(region);
			if (bloomFilter.mightContain(species)) {
				logger.info("{} found in the BloomFilter. Going to DB for further check.", species);
				return isInvasiveInDB(species, region);
			} else {
				logger.info("{} didn't find in the BloomFilter. It is not an invasive species in {}", species, region);
				return false;
			}
		} else {
			// TODO : Update numbers below based on number of elements

			// number of elements = 40, probablity of false positives = 0.0000001 (1 in
			// 10000000)
			// nummber of bits (size of bloom filter) = 25794 (3.15KiB), number of hash
			// functions = 3
			BloomFilter bloomFilter = new BloomFilter(25794);

			var introducedSpeciesCollection = speciesAttributesDB.getCollection("introduced-species");
			var introducedSpecies = introducedSpeciesCollection.find();

			boolean isIntroducedSpecies = false;
			for (var iSpecies : introducedSpecies) {
				var speciesName = iSpecies.getString("name");
				bloomFilter.add(speciesName);
				if (speciesName.equals(species))
					isIntroducedSpecies = true;
			}

			bloomFilters.put(region, bloomFilter);
			return isIntroducedSpecies;
		}
	}

	private boolean isInvasiveInDB(String species, String region) {
		var invasiveSpeciesCollection = speciesAttributesDB.getCollection("invasive_species");

		Bson filter = Filters.eq("name", species);
		long recordCount = invasiveSpeciesCollection.countDocuments(filter);
		if (recordCount > 0)
			logger.info("{} found in DB. It is an invasive species in {}.", species, region);
		else
			logger.info("{} not found in DB. It is not an invasive species in {}.", species, region);
		return recordCount > 0;
	}
}
