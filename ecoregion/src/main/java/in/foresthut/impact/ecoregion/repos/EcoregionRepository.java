package in.foresthut.impact.ecoregion.repos;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import in.foresthut.impact.ecoregion.entities.Ecoregion;

/**
 * @author maulik-dave
 */
public interface EcoregionRepository extends MongoRepository<Ecoregion, String> {
	@Query("{ 'regionMap': { $geoIntersects: { $geometry: ?0 } } }")
	public Ecoregion findEcoregionOf(GeoJsonPoint point);
}
