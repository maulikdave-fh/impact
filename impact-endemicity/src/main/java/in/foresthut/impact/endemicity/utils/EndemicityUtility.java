package in.foresthut.impact.endemicity.utils;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.foresthut.impact.endemicity.infra.GBIFClient;

public class EndemicityUtility {
	private static EndemicityUtility instance;
	private static final Logger logger = LoggerFactory.getLogger(EndemicityUtility.class);
	private static final Map<String, Map<String, Double>> endemicityCache = new HashMap<>();

	private EndemicityUtility() {

	}

	public static EndemicityUtility getInstance() {
		if (instance == null)
			instance = new EndemicityUtility();
		return instance;
	}

	public double endemicity(String species, String regionMap) {
		if (endemicityCache.containsKey(regionMap))
			if (endemicityCache.get(regionMap).containsKey(species))
				return endemicityCache.get(regionMap).get(species);

		GBIFClient gbifClient = GBIFClient.getInstance();
		// Fetch species observation count for the region
		long regionCount = gbifClient.observations(species, regionMap);

		// Fetch global species observation count
		long globalCount = gbifClient.observations(species, null);
		double endemicity = (double) regionCount / globalCount;
		logger.info("Species: {}, Endemicity: {}", species, endemicity);
		return endemicity;
	}
}
