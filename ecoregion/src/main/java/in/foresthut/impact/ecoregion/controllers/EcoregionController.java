package in.foresthut.impact.ecoregion.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.foresthut.impact.ecoregion.entities.Ecoregion;
import in.foresthut.impact.ecoregion.services.EcoregionService;

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
	@RequestMapping("/ecoregion/{latitude}/{longitude}")
	public ResponseEntity<Ecoregion> of(@PathVariable Double latitude, @PathVariable Double longitude) {
		Ecoregion region = ecoregionService.of(latitude, longitude);
		log.info("[{}, {}] belongs to ecoregion \"{}\"", latitude, longitude,
				region == null ? "NONE" : region.getName());
		return region == null ? new ResponseEntity<>(null, HttpStatus.NOT_FOUND)
				: new ResponseEntity<>(region, HttpStatus.OK);
	}
}
