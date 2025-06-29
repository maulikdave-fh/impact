package in.foresthut.impact.endemicity.infra;

import java.io.UnsupportedEncodingException;
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

public class GeminiClient {
	private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

	private final static Config config = Config.getInstance();
	private final static String GEMINI_PROXY_HOST = config.get("gemini.service");

	private final HttpClient client;
	private final ObjectMapper objectMapper;

	private static GeminiClient instance;

	public synchronized static GeminiClient getInstance() {
		if (instance == null)
			instance = new GeminiClient();
		return instance;
	}

	private GeminiClient() {
		client = HttpClient.newBuilder().build();
		objectMapper = new ObjectMapper();
	}

	public String send(String prompt) {
		String result = new String();
		String requestUrl;
		try {
			requestUrl = GEMINI_PROXY_HOST + "text=" + URLEncoder.encode(prompt, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported Encoding UTF-8 for prompt {}", prompt);
			throw new AlreadyLoggedException(String.format("Unsupported Encoding UTF-8 for %s", prompt), null, e);
		}

		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				result = objectMapper.readValue(response.body(), String.class);
			} else {
				logger.error("HTTP error {} for prompt {}", response.statusCode(), prompt);
				throw new AlreadyLoggedException(
						String.format("Http error %d for task %s", response.statusCode(), prompt), null, null);
			}
		} catch (Exception e) {
			logger.error("Error while getting response for URL {}", requestUrl, e);
			throw new AlreadyLoggedException(String.format("Error while getting response for URL %s", requestUrl), null,
					e);
		}
		logger.info("Received {} for prompt '{}'", result, prompt);
		return result;
	}
}
