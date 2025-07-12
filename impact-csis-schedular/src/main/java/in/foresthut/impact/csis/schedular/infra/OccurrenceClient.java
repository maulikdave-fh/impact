package in.foresthut.impact.csis.schedular.infra;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Date;

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

	public String latestModifiedOn(String regionName, String ecoregionId) {
		String OCCURRENCE_MODIFIEDON_SERVICE_URL = null;
		try {
			OCCURRENCE_MODIFIEDON_SERVICE_URL = OCCURRENCE_HOST + "/modifiedOn/"
					+ URLEncoder.encode(regionName, "UTF-8") + "/" + ecoregionId;
		} catch (Exception ex) {
			logger.error("Encoding error for regionName {} in url {}", regionName, OCCURRENCE_MODIFIEDON_SERVICE_URL);
		}
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(OCCURRENCE_MODIFIEDON_SERVICE_URL)).GET().build();
		Long result = null;
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			result = objectMapper.readValue(response.body(), Long.class);
			logger.info("An occurrence with the latest date {} found for ecoregion {}", result, ecoregionId);
		} catch (Exception ex) {
			logger.error("Error while fetching the latest date for ecorgion {} in region {}", ecoregionId, regionName,
					ex);
			throw new AlreadyLoggedException(
					String.format("Error while fetching the latest date for ecoregion %s for region %s", ecoregionId,
							regionName),
					null, ex);
		}
		// 1. Create a Date object from the long milliseconds
        Date date = new Date(result);

        // 2. Create a SimpleDateFormat object with the desired format pattern
        //    For example, "yyyy-MM-dd HH:mm:ss" for "2023-03-15 00:00:00"
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // 3. Format the Date object into a String
        String dateString = formatter.format(date);
		return dateString;
	}

}
