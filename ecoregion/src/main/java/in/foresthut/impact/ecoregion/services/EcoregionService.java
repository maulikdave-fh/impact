package in.foresthut.impact.ecoregion.services;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import in.foresthut.impact.ecoregion.entities.Ecoregion;
import in.foresthut.impact.ecoregion.repos.EcoregionRepository;

/**
 * @author maulik-dave
 */
// TODO : [Performance + Cloud Cost] Caching of eco-regions
// TODO : [Performance + Cloud Cost] Application layer geospatial operation - point in polygon
@Service
public class EcoregionService {

	private EcoregionRepository ecoregionRepo;

	public EcoregionService(EcoregionRepository ecoregionRepository) {
		this.ecoregionRepo = ecoregionRepository;
	}

	public Ecoregion of(Double latitude, Double longitude) {
		Ecoregion region = ecoregionRepo.findEcoregionOf(new GeoJsonPoint(longitude, latitude));
		return region;
	}
}
