package in.foresthut.impact.gbif;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
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

	// TODO - send a page retrieval request in separate threads once number of pages
	// are known
	public List<Observation> observations(Task task) throws IOException, InterruptedException, ExecutionException {
		List<Observation> observations = new ArrayList<>();
		int offset = 0;
		String gbifUrl = GBIF_HOST + "/occurrence/search?occurrenceStatus=PRESENT&limit=" + LIMIT + "&offset=" + offset
				+ "&modified=" + URLEncoder.encode(task.dateFrom(), "UTF-8") + ","
				+ URLEncoder.encode(task.dateTo(), "UTF-8") + "&geometry=" + URLEncoder.encode(task.polygon(), "UTF-8");

		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(gbifUrl))
					.header("User-Agent", "sayhello@foresthut.in").GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				GBIFResponse gbifResponse = objectMapper.readValue(response.body(), GBIFResponse.class);
				logger.info("Total {} observations to be fetched.", gbifResponse.count());
				observations.addAll(gbifResponse.results());
				logger.info("Received {} observations for page number {}", gbifResponse.results().size(),
						(offset + LIMIT) / LIMIT);

				if (!gbifResponse.endOfRecords()) {
					for (int i = offset + LIMIT; i < gbifResponse.count(); i += LIMIT) {
						gbifUrl = GBIF_HOST + "/occurrence/search?occurrenceStatus=PRESENT&limit=" + LIMIT + "&offset="
								+ i + "&modified=" + URLEncoder.encode(task.dateFrom(), "UTF-8") + ","
								+ URLEncoder.encode(task.dateTo(), "UTF-8") + "&geometry="
								+ URLEncoder.encode(task.polygon(), "UTF-8");
						try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
							Future<List<Observation>> future = executor
									.submit(new FetchGBIFObservationsTask(client, gbifUrl, task, i));
							observations.addAll(future.get());
						}
					}
				}

			} else {
				logger.error("HTTP error {} for task {}", response.statusCode(), task);
				throw new IOException(String.format("Http error %d for task %s", response.statusCode(), task));
			}
		} catch (Exception e) {
			logger.error("Error while getting response for URL {}", gbifUrl, e);
			throw e;
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

	static class FetchGBIFObservationsTask implements Callable<List<Observation>> {
		private HttpClient client;
		private String url;
		private Task task;
		private int offset;
		private final ObjectMapper objectMapper;

		public FetchGBIFObservationsTask(HttpClient client, String url, Task task, int offset) {
			this.client = client;
			this.url = url;
			this.task = task;
			this.offset = offset;
			this.objectMapper = new ObjectMapper();
		}

		@Override
		public List<Observation> call() throws Exception {
			List<Observation> observations = new ArrayList<>();
			try {
				HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
						.header("User-Agent", "sayhello@foresthut.in").GET().build();
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					GBIFResponse gbifResponse = objectMapper.readValue(response.body(), GBIFResponse.class);
					observations = gbifResponse.results();
					logger.info("Received {} observations for page number {}", observations.size(),
							(offset / LIMIT) + 1);
				} else {
					logger.error("HTTP error {} for task {}", response.statusCode(), task);
					throw new IOException(String.format("Http error %d for task %s", response.statusCode(), task));
				}
			} catch (Exception e) {
				logger.error("Error while getting response for URL {}", url, e);
				throw e;
			}
			return observations;
		}

	}

}
