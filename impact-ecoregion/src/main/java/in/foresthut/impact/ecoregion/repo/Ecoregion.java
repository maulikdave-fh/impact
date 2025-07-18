package in.foresthut.impact.ecoregion.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import in.foresthut.impact.ecoregion.exceptions.InvalidEcoregionIdException;
import in.foresthut.impact.ecoregion.infra.Cache;
import in.foresthut.impact.utils.geometry.Polygon;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Ecoregion(String id, int regionId, String name, String realm, String biome, String bioregion,
		String region, int areaHectares) {

	private static MongoDatabase earthDatabase = EarthDatabase.getInstance();
	private static final Logger logger = LoggerFactory.getLogger(Ecoregion.class);

	/**
	 * Returns ecoregion polygon in WKT (Well Known Text) format.
	 * 
	 * @param ecoregion id
	 * @return ecoregion polygon in WKT format
	 * @throws InvalidEcoregionIdException
	 */
	private static String getRegionMap(String id) throws InvalidEcoregionIdException {
		var ecoregionCollection = earthDatabase.getCollection("ecoregion");
		Bson includeProjection = Projections.include("regionMap");
		Bson eqFilter = null;
		try {
			eqFilter = Filters.eq("_id", new ObjectId(id));
		} catch (Exception ex) {
			final String traceId = UUID.randomUUID().toString();
			logger.error("[Error-trace-id] {} Invalid ecoregion id {}", traceId, id, ex);
			throw new InvalidEcoregionIdException("Invalid ecoregion Id", traceId, ex);
		}
		var ecoregion = ecoregionCollection.find(eqFilter).projection(includeProjection).first();
		if (ecoregion == null) {
			logger.info("No ecoregion found for id {}", id);
			return null;
		} else {
			return ecoregion.getString("regionMap");
		}
	}

	public static List<String> split(String id) throws InvalidEcoregionIdException {
		// check in cache if the split exists for given id
		Cache cache = Cache.getInstance();
		var splits = cache.splits(id);
		if (splits != null) {
			logger.info("Ecoregion {} splits found in the cache.", id);
			return (List<String>) splits;
		}

		// otherwise fetch ecoregion for given id
		logger.info("Ecoregion {} splits not found in cache.", id);
		var ecoregionMap = getRegionMap(id);
		if (ecoregionMap != null) {
			List<String> wktPolys = new ArrayList<>();
			wktPolys = new Polygon(ecoregionMap).split();
			logger.info("Ecoregion {} was split into {} parts.", id, wktPolys.size());
			cache.add(id, wktPolys);
			logger.info("Cache updated for ecoregion {} splits.", id);
			return wktPolys;
		} else {
			logger.error("Ecoregion with id {} not found", id);
			return new ArrayList<String>();
		}
	}

	static Ecoregion from(Document ecoregion) {
		Ecoregion region = new Ecoregion(ecoregion.getObjectId("_id").toString(), ecoregion.getInteger("regionId"),
				ecoregion.getString("name"), ecoregion.getString("realm"), ecoregion.getString("biome"),
				ecoregion.getString("bioregion"), ecoregion.getString("region"),ecoregion.getInteger("areaHectares"));
		return region;
	}
}
