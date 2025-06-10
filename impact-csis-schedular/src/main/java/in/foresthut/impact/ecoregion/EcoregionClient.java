package in.foresthut.impact.ecoregion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	private record Ecoregion(String id) {
		@Override
		public String toString() {
			return id();
		}
	}

	public EcoregionClient() {
		client = HttpClient.newHttpClient();
		objectMapper = new ObjectMapper();
	}

	public List<String> ecoregions() throws IOException, InterruptedException {
		final String ECOREGIONS_URL = ECOREGION_HOST + "/ecoregions";

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ECOREGIONS_URL)).GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		List<Ecoregion> ecoregions = objectMapper.readValue(response.body(), new TypeReference<>() {
		});
		logger.info("Found {} ecoregions.", ecoregions.size());
		return ecoregions.stream().map(e -> e.toString()).toList();
	}

	public List<String> split(String ecoregionId) throws IOException, InterruptedException, ExecutionException {

		List<String> result = new ArrayList<>();

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Future<List<String>> future = executor.submit(new SplitEcoregionTask(client, ecoregionId));
			result.addAll(future.get());
		}
		return result;
	}

	static class SplitEcoregionTask implements Callable<List<String>> {
		private HttpClient client;
		private String ecoregionId;

		public SplitEcoregionTask(HttpClient client, String ecoregionId) {
			this.client = client;
			this.ecoregionId = ecoregionId;
		}

		@Override
		public List<String> call() throws Exception {
			final String ECOREGION_URL = ECOREGION_HOST + "/ecoregion/" + ecoregionId + "/split";
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ECOREGION_URL)).GET().build();
			HttpResponse<String> response;
			try {
				response = client.send(request, HttpResponse.BodyHandlers.ofString());

				if (response.statusCode() == 404)
					throw new IOException("ecoregion not found");
				return new ObjectMapper().readValue(response.body(), new TypeReference<>() {
				});
			} catch (IOException | InterruptedException e) {
				logger.error("Error while fetching ecoregion splits for {}", ecoregionId, e);
			}
			return null;
		}
	}
}
