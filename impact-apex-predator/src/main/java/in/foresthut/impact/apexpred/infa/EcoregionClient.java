package in.foresthut.impact.apexpred.infa;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.config.Config;

/**
 * Responsible to communicate with impact-ecoregion service.
 * 
 * @author maulik-dave
 */
public class EcoregionClient {
	private static final Logger logger = LoggerFactory.getLogger(EcoregionClient.class);

	private final static Config config = Config.getInstance();
	private final static String ECOREGION_HOST = config.get("ecoregion.host");

	private final HttpClient client;
	private final ObjectMapper objectMapper;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Ecoregion(String id, String region) {
	}

	public EcoregionClient() {
		client = HttpClient.newHttpClient();
		objectMapper = new ObjectMapper();
	}

	public Map<String, String> ecoregions() {
		final String ECOREGIONS_URL = ECOREGION_HOST + "/ecoregions";
		List<Ecoregion> ecoregions = new ArrayList<>();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ECOREGIONS_URL)).GET().build();
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			ecoregions = objectMapper.readValue(response.body(), new TypeReference<>() {
			});
			logger.info("Found {} ecoregions.", ecoregions.size());
		} catch (Exception ex) {
			logger.error("Error while fetching ecoregions", ex);
			throw new AlreadyLoggedException("Error while fetching ecoregions", null, ex);
		}
		Map<String, String> map = new HashMap<>();
		for (var ecoregion : ecoregions)
			map.put(ecoregion.id(), ecoregion.region());
		return map;
	}
}
