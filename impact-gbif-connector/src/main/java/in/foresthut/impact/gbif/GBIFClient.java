package in.foresthut.impact.gbif;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.foresthut.impact.Config;
import in.foresthut.impact.Task;

public class GBIFClient {
	private static final Logger logger = LoggerFactory.getLogger(GBIFClient.class);

	private final static Config config = Config.getInstance();
	private final static String GBIF_HOST = config.get("gbif.host");

	private final HttpClient client;
	private final ObjectMapper objectMapper;

	public GBIFClient() {
		client = HttpClient.newHttpClient();
		objectMapper = new ObjectMapper();
	}

	public Observations observations(Task task) throws IOException, InterruptedException, ExecutionException {
		final String GBIF_URL = GBIF_HOST + "/occurrence/search?modified=" + URLEncoder.encode(task.date(), "UTF-8")
				+ "&geometry=" + URLEncoder.encode(task.polygon(), "UTF-8");

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GBIF_URL)).GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		Observations observations = objectMapper.readValue(response.body(), Observations.class);
		logger.info("Found {} observations.", observations.count());

		return observations;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static record Observations(int offset, int limit, boolean endOfRecords, int count,
			List<Observation> results) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static record Observation(String iucnRedListCategory, String species) {
	}

}
