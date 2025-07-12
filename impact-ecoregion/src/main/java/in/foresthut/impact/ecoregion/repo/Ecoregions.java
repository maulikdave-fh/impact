package in.foresthut.impact.ecoregion.repo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

import in.foresthut.impact.utils.geometry.Polygon;

public class Ecoregions {
	private static MongoDatabase earthDatabase = EarthDatabase.getInstance();
	private static final Logger logger = LoggerFactory.getLogger(Ecoregions.class);

	private Ecoregions() {

	}

	public static List<Ecoregion> get() {
		List<Ecoregion> ecoregions = new ArrayList<>();
		var ecoregionCollection = earthDatabase.getCollection("ecoregion");
		Bson includeProjection = Projections.include("id", "regionId", "name", "realm", "biome", "bioregion", "region",
				"areaHectares");
		for (Document ecoregion : ecoregionCollection.find().projection(includeProjection)) {
			ecoregions.add(Ecoregion.from(ecoregion));
		}
		logger.info("Found {} ecoregion(s).", ecoregions.size());
		return ecoregions;
	}

	public static EcoregionMap ecoregionContaining(double latitude, double longitude) {
		var ecoregionCollection = earthDatabase.getCollection("ecoregion");
		Bson includeProjection = Projections.include("id", "region", "regionMap");
		for (Document ecoregion : ecoregionCollection.find().projection(includeProjection)) {
			var polygon = new Polygon(ecoregion.getString("regionMap"));
			if (polygon.contains(latitude, longitude)) {
				return new EcoregionMap(ecoregion.getObjectId("_id").toString(), ecoregion.getString("region"));
			}			
		}
		return null;
	}

	public record EcoregionMap(String id, String bioregionName) {
	}
}
