package in.foresthut.impact.endemicity.infra;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	public List<Ecoregion> ecoregions() throws IOException, InterruptedException {
		final String ECOREGIONS_URL = ECOREGION_HOST + "/ecoregions";

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ECOREGIONS_URL)).GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		List<Ecoregion> ecoregions = objectMapper.readValue(response.body(), new TypeReference<>() {
		});
		logger.info("Found {} ecoregions.", ecoregions.size());
		return ecoregions;
	}
}
