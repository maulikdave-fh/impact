package in.foresthut.impact.ecoregion.repo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

import in.foresthut.impact.ecoregion.infra.EarthDatabase;

public class Ecoregions {
	private static MongoDatabase earthDatabase;
	private static final Logger logger = LoggerFactory.getLogger(Ecoregions.class);

	private Ecoregions() {

	}

	public static List<Ecoregion> get() {
		earthDatabase = EarthDatabase.getInstance();
		List<Ecoregion> ecoregions = new ArrayList<>();
		var ecoregionCollection = earthDatabase.getCollection("ecoregion");
		Bson includeProjection = Projections.include("id", "regionId", "name", "realm", "biome", "bioregion",
				"keyStoneSpecies", "areaHectares");
		for (Document ecoregion : ecoregionCollection.find().projection(includeProjection)) {
			ecoregions.add(Ecoregion.from(ecoregion));
		}
		logger.info("Found {} ecoregion(s).", ecoregions.size());
		return ecoregions;
	}
}
