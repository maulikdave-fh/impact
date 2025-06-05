package in.foresthut.impact.ecoregion.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import in.foresthut.impact.ecoregion.entities.Ecoregion;
import in.foresthut.impact.ecoregion.repos.EcoregionRepository;
import in.foresthut.impact.ecoregion.vos.PartOfEcoregion;
import in.foresthut.impact.utils.Polygon;
import in.foresthut.impact.utils.exceptions.InvalidGeoJsonException;

/**
 * @author maulik-dave
 */
// TODO : [Performance + Cloud Cost] Caching of eco-regions
// TODO : [Performance + Cloud Cost] Application layer geospatial operations - point in polygon
@Service
public class EcoregionService {
	private static Logger log = LoggerFactory.getLogger(EcoregionService.class);
	private EcoregionRepository ecoregionRepo;

	private Map<Integer, Ecoregion> ecoregionCache = new HashMap<>();
	private Map<Integer, List<PartOfEcoregion>> ecoregionPartsCache = new HashMap<>();

	public EcoregionService(EcoregionRepository ecoregionRepository) {
		this.ecoregionRepo = ecoregionRepository;
	}

	public Ecoregion of(Double latitude, Double longitude) {
		List<Ecoregion> ecoregions = all();
		for (var ecoregion : ecoregions) {
			try {
				var ecoregionPoly = new Polygon(ecoregion.getRegionMap());
				if (ecoregionPoly.contains(latitude, longitude)) {
					return ecoregion;
				}
			} catch (InvalidGeoJsonException e) {
				log.error("Error parsing geoJSON for {}", ecoregion.getRegionId(), e);
			}
		}
		return null;
	}

	public List<Ecoregion> all() {
		if (ecoregionCache.isEmpty()) {
			List<Ecoregion> ecoregions = ecoregionRepo.findAll();
			for (var ecoregion : ecoregions)
				ecoregionCache.put(ecoregion.getRegionId(), ecoregion);
		}
		return new ArrayList<>(ecoregionCache.values());
	}

	public List<PartOfEcoregion> split(int ecoregionId) {
		if (ecoregionPartsCache.get(ecoregionId) != null) {
			return ecoregionPartsCache.get(ecoregionId);
		}

		Ecoregion ecoregion = null;
		if (ecoregionCache.get(ecoregionId) == null) {
			ecoregion = ecoregionRepo.findByRegionId(ecoregionId);
			if (ecoregion == null)
				return null;
		} else {
			ecoregion = ecoregionCache.get(ecoregionId);
		}

		List<PartOfEcoregion> parts = new ArrayList<>();
		List<String> wktPolys = new ArrayList<>();
		try {
			wktPolys = new Polygon(ecoregion.getRegionMap()).split();
		} catch (InvalidGeoJsonException ex) {
			log.error("Error parsing geoJson of ecoregion {}", ecoregion.getRegionId(), ex);
		}

		for (var wktPoly : wktPolys) {
			parts.add(new PartOfEcoregion(ecoregion.getId(), ecoregion.getRegionId(), ecoregion.getName(), wktPoly));
		}
		log.info("ecoregion {} was split into {} parts.", ecoregion.getRegionId(), wktPolys.size());
		ecoregionPartsCache.put(ecoregion.getRegionId(), parts);
		return parts;
	}
}
