package in.foresthut.impact.ecoregion.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.foresthut.impact.ecoregion.entities.Ecoregion;
import in.foresthut.impact.ecoregion.services.EcoregionService;
import in.foresthut.impact.ecoregion.vos.PartOfEcoregion;

/**
 * This class exposes API endpoints to work with ecoregions.
 * 
 * @author maulik-dave
 */
@RestController
public class EcoregionController {

	private static Logger log = LoggerFactory.getLogger(EcoregionController.class);
	private EcoregionService ecoregionService;

	public EcoregionController(EcoregionService ecoregionService) {
		this.ecoregionService = ecoregionService;
	}

	/**
	 * Retrieves ecoregion of the given point. The point is represented by latitude
	 * and longitude.
	 * 
	 * @param latitude
	 * @param longitude
	 * @return Ecoregion. Returns 404, if the point doesn't fall into any of the
	 *         supported ecoregions
	 */
	@RequestMapping("/ecoregions/{latitude}/{longitude}")
	public ResponseEntity<Ecoregion> of(@PathVariable Double latitude, @PathVariable Double longitude) {
		Ecoregion region = ecoregionService.of(latitude, longitude);
		log.info("[{}, {}] belongs to ecoregion \"{}\"", latitude, longitude,
				region == null ? "NONE" : region.getName());
		return region == null ? new ResponseEntity<>(null, HttpStatus.NOT_FOUND)
				: new ResponseEntity<>(region, HttpStatus.OK);
	}

	/**
	 * Retrieves all available ecoregions.
	 * 
	 * @return list of available ecoregions.
	 */

	@RequestMapping("/ecoregions")
	public ResponseEntity<List<Ecoregion>> all() {
		return new ResponseEntity<>(ecoregionService.all(), HttpStatus.OK);
	}

	/**
	 * Splits given ecoregion into pre-configured size by area.
	 * 
	 * @return list of parts of ecoregion. A polygon is represented in WKT format
	 */
	@RequestMapping("/ecoregions/{regionId}/split")
	public ResponseEntity<List<PartOfEcoregion>> split(@PathVariable int regionId) {
		var result = ecoregionService.split(regionId);
		log.info("ecoregion with regionId {}  {}", regionId,
				result == null ? "not found" : "was split into " + result.size() + " polygons.");
		return result == null ? new ResponseEntity<>(null, HttpStatus.NOT_FOUND)
				: new ResponseEntity<>(result, HttpStatus.OK);
	}
}
