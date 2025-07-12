package in.foresthut.impact.gbif.data.processor.infa;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.config.Config;

/**
 * Responsible to communicate with impact-ecoregion service.
 * 
 * @author maulik-dave
 */
public class EcoregionClient {
	private static EcoregionClient instance;
	private static final Logger logger = LoggerFactory.getLogger(EcoregionClient.class);

	private final static Config config = Config.getInstance();
	private final static String ECOREGION_HOST = config.get("ecoregion.host");

	private final HttpClient client;
	private final ObjectMapper objectMapper;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Ecoregion(String id, String bioregionName) {
	}

	private EcoregionClient() {
		client = HttpClient.newHttpClient();
		objectMapper = new ObjectMapper();
	}
	
	public static EcoregionClient getInstance() {
		if (instance == null)
			instance = new EcoregionClient();
		return instance;
	}

	public Ecoregion getEcoregion(double latitude, double longitude) {
		final String ECOREGIONS_URL = ECOREGION_HOST + "/ecoregionid/for/"+latitude+"/"+longitude;
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ECOREGIONS_URL)).GET().build();
		Ecoregion ecoregion = null;
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			ecoregion = objectMapper.readValue(response.body(), Ecoregion.class);
//			logger.info("The coordinates ({}, {}) fall in ecoregion {}", latitude, longitude,
//					ecoregionId);
		} catch (Exception ex) {
			logger.error("Error while fetching ecoregion id", ex);
			throw new AlreadyLoggedException("Error while fetching ecoregion id", null, ex);
		}
		return ecoregion;
	}
}
