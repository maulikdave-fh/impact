package in.foresthut.impact.endemicity.infra;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.config.Config;

public class GBIFClient {
	private static final Logger logger = LoggerFactory.getLogger(GBIFClient.class);

	private final static Config config = Config.getInstance();
	private final static String GBIF_HOST = config.get("gbif.host");

	private final static int LIMIT = 1;
	private final static String DATASET_KEY = "50c9509d-22c7-4a22-a47d-8c48425ef4a7"; // iNaturalist research grade
																						// obsevations

	private final HttpClient client;
	private final ObjectMapper objectMapper;

	private static GBIFClient instance;

	public synchronized static GBIFClient getInstance() {
		if (instance == null)
			instance = new GBIFClient();
		return instance;
	}

	private GBIFClient() {
		client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
		objectMapper = new ObjectMapper();
	}

	public long observations(String species, String polygon) {
		String gbifUrl = new String();
		try {
		gbifUrl = GBIF_HOST + "/occurrence/search?occurrenceStatus=PRESENT&limit=" + LIMIT + "&datasetKey="
				+ URLEncoder.encode(DATASET_KEY, "UTF-8") + "&scientificName=" + URLEncoder.encode(species, "UTF-8");

		if (polygon != null)
			gbifUrl += "&geometry=" + URLEncoder.encode(polygon, "UTF-8");
		} catch(UnsupportedEncodingException ex) {
			logger.error("Encoding not supported for URL {}", gbifUrl, ex);
			throw new AlreadyLoggedException("Encoding not supported for URL " + gbifUrl, null, ex);
		}
		
		GBIFResponse gbifResponse;
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(gbifUrl))
					.header("User-Agent", "sayhello@foresthut.in").GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				gbifResponse = objectMapper.readValue(response.body(), GBIFResponse.class);
				logger.info("Total {} observations found for {} in polygon {}.", gbifResponse.count(), species,
						polygon);
			} else {
				logger.error("HTTP error {} for species{} and polygon {}", response.statusCode(), species, polygon);
				throw new AlreadyLoggedException(String.format("Http error %d for species %s and polygon %s",
						response.statusCode(), species, polygon), null, null);
			}
		} catch (Exception e) {
			logger.error("Error while getting response for URL {}", gbifUrl, e);
			throw new AlreadyLoggedException("Error while getting response for URL " + gbifUrl, null, e);
		}

		return gbifResponse.count();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static record GBIFResponse(long count) {
	}
}
