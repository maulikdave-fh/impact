package in.foresthut.impact.ecoregion.repos;

import org.springframework.data.mongodb.repository.MongoRepository;

import in.foresthut.impact.ecoregion.entities.Ecoregion;

/**
 * @author maulik-dave
 */
public interface EcoregionRepository extends MongoRepository<Ecoregion, String> {

	public Ecoregion findByRegionId(int ecoregionId);
//	@Query("{ 'regionMap': { $geoIntersects: { $geometry: ?0 } } }")
//	public Ecoregion findEcoregionOf(GeoJsonPoint point);	
}
