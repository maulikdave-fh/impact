package in.foresthut.impact.gbif.data.processor.infa;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.config.Config;

public class OccurrenceClient {
	private static final Logger logger = LoggerFactory.getLogger(OccurrenceClient.class);

	private final static Config config = Config.getInstance();
	private final static String OCCURRENCE_HOST = config.get("occurrence.host");

	private static OccurrenceClient instance;

	private final HttpClient client;
	private final ObjectMapper objectMapper;

	private OccurrenceClient() {
		client = HttpClient.newHttpClient();
		objectMapper = new ObjectMapper();
	}

	public static OccurrenceClient getInstance() {
		if (instance == null)
			instance = new OccurrenceClient();
		return instance;
	}

	public boolean exists(String regionName, long key) {
		String OCCURRENCE_EXISTS_SERVICE_URL = null;
		try {
			OCCURRENCE_EXISTS_SERVICE_URL = OCCURRENCE_HOST + "/occurrences/" + URLEncoder.encode(regionName, "UTF-8")
					+ "/" + key + "/exists";
		} catch (Exception ex) {
			logger.error("Encoding error for regionName {} in url {}", regionName, OCCURRENCE_EXISTS_SERVICE_URL);
		}
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(OCCURRENCE_EXISTS_SERVICE_URL)).GET().build();
		boolean exists = false;
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			exists = objectMapper.readValue(response.body(), Boolean.class);
			logger.info("An occurrence with gbifKey {} exists: {}", key, exists);
//			logger.info("The coordinates ({}, {}) fall in ecoregion {}", latitude, longitude,
//					ecoregionId);
		} catch (Exception ex) {
			logger.error("Error while checking if gbifkey {} exists for region {}", key, regionName, ex);
			throw new AlreadyLoggedException(
					String.format("Error while checking if gbifkey %s exists for region %s", key, regionName), null,
					ex);
		}
		return exists;
	}

}
