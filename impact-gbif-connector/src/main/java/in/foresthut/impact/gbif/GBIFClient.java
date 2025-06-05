package in.foresthut.impact.gbif;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.foresthut.impact.Config;
import in.foresthut.impact.Task;

public class GBIFClient {
	private static final Logger logger = LoggerFactory.getLogger(GBIFClient.class);

	private final static Config config = Config.getInstance();
	private final static String GBIF_HOST = config.get("gbif.host");

	private final static int LIMIT = 300;

	private final HttpClient client;
	private final ObjectMapper objectMapper;

	public GBIFClient() {
		client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
		objectMapper = new ObjectMapper();
	}

	public List<Observation> observations(Task task) throws IOException, InterruptedException, ExecutionException {
		List<Observation> observations = new ArrayList<>();
		boolean endOfRecords = false;
		int offset = 0;
		while (!endOfRecords) {
			final String GBIF_URL = GBIF_HOST + "/occurrence/search?limit=" + LIMIT + "&offset=" + offset + "&modified="
					+ URLEncoder.encode(task.dateFrom(), "UTF-8") + "," + URLEncoder.encode(task.dateTo(), "UTF-8")
					+ "&geometry=" + URLEncoder.encode(task.polygon(), "UTF-8");

			try {
				HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GBIF_URL))
						.header("User-Agent", "sayhello@foresthut.in").GET().build();
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
				GBIFResponse gbifResponse = objectMapper.readValue(response.body(), GBIFResponse.class);
				observations.addAll(gbifResponse.results());
				endOfRecords = gbifResponse.endOfRecords();
				offset += LIMIT;
				logger.info("Received {} observations for page number {}", gbifResponse.results().size(),
						offset / LIMIT);
			} catch (Exception e) {
				logger.error("Error while getting response for URL {}", GBIF_URL, e);
				throw e;
			}
		}
		logger.info("Received {} observations for task {}", observations.size(), task);
		return observations;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static record GBIFResponse(int offset, int limit, boolean endOfRecords, int count,
			List<Observation> results) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static record Observation(long key, String species, String kingdom, String phylum, String order,
			@JsonProperty(value = "class") String _class, String iucnRedListCategory, double decimalLatitude,
			double decimalLongitude, String eventDate, String modified, String datasetName) {
	}

}
