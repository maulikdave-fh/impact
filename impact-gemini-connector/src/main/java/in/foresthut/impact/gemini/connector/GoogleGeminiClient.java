package in.foresthut.impact.gemini.connector;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Part;
import com.google.genai.types.Tool;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.gemini.exceptions.CallLimitExceededException;
import in.foresthut.impact.gemini.infra.CallCounterDatabaseConfig;

public class GoogleGeminiClient {
	private static final Logger logger = LoggerFactory.getLogger(GoogleGeminiClient.class);

	private final static Config config = Config.getInstance();
	private final static int LIMIT = Integer.valueOf(config.get("gemini.api.call.limit.perday"));

	private final String GEMINI_API_KEY;
	private final Client client;

	private final static CallCounterDatabaseConfig callCounterDBConfig = CallCounterDatabaseConfig.getInstance();
	private final static MongoClient mongoClient = callCounterDBConfig.mongoClient();
	private final static MongoDatabase callCounterDB = callCounterDBConfig.database();

	private static GoogleGeminiClient instance;

	public synchronized static GoogleGeminiClient getInstance() {
		if (instance == null)
			instance = new GoogleGeminiClient();
		return instance;
	}

	private GoogleGeminiClient() {
		GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
		if (GEMINI_API_KEY == null) {
			logger.error("GEMINI_API_KEY not found");
			throw new RuntimeException("GEMINI_API_KEY not found");
		}
		client = Client.builder().apiKey(System.getenv("GEMINI_API_KEY")).build();
	}

	private synchronized AtomicInteger counter() {
		var counterCollection = callCounterDB.getCollection("counter");

		// Create a session
		ClientSession session = mongoClient.startSession();

		// Start a transaction
		session.startTransaction();

		LocalDate today = LocalDate.now();
		AtomicInteger counter = new AtomicInteger(0);
		try {
			Bson filter = Filters.eq("date", today);
			Document record = counterCollection.find(filter).first();

			if (record == null) {
				counterCollection.insertOne(new Document("date", today).append("calls", 0));
				counter = new AtomicInteger(0);
			} else {
				counter = new AtomicInteger(record.getInteger("calls"));
			}
		} catch (Exception e) {
			session.abortTransaction();
			logger.error("Transaction aborted due to error {}", e.getMessage(), e);
		} finally {
			session.close();
		}
		return counter;
	}

	public String send(String[] texts, boolean googleSearch) {
		synchronized (this) {
			AtomicInteger counter = counter();
			if (counter.get() >= LIMIT) {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Call limit exceeded '{}'", traceId, LIMIT);
				throw new CallLimitExceededException(
						String.format("Allowed call limit %s to Gemini APIs exceeded", LIMIT), traceId, null);
			}
		}

		String response = new String();

		logger.info("Sending request to Gemini for prompt '{}'", Arrays.toString(texts));

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Future<String> future = executor.submit(new FetchGeminiResponseTask(client, texts));
			response = future.get();
			synchronized (this) {
				if (!response.isEmpty()) {
					incrementCounterInDB();
				}
			}
		} catch (Exception e) {
			logger.error("Error while comminicating with Google Gemini API", e);
			throw new RuntimeException("Error while comminicating with Google Gemini API", e);

		}
		logger.info("Received response successfully from Gemini for prompt '{}'", Arrays.toString(texts));
		return response;
	}

	private void incrementCounterInDB() {
		var counterCollection = callCounterDB.getCollection("counter");

		// Create a session
		ClientSession session = mongoClient.startSession();

		// Start a transaction
		session.startTransaction();

		LocalDate today = LocalDate.now();
		try {
			Bson filter = Filters.eq("date", today);
			Document record = counterCollection.find(filter).first();

			if (record == null) {
				counterCollection.insertOne(new Document("date", today).append("calls", 0));
			} else {
				var currentCounter = counter();
				Bson updateCalls = Updates.set("calls", currentCounter.addAndGet(1));
				counterCollection.updateOne(filter, updateCalls);
			}
		} catch (Exception e) {
			session.abortTransaction();
			logger.error("Transaction aborted due to error {}", e.getMessage(), e);
		} finally {
			session.close();
		}
	}

	static class FetchGeminiResponseTask implements Callable<String> {
		private Client client;
		private String[] inputTexts;

		public FetchGeminiResponseTask(Client client, String[] inputTexts) {
			this.client = client;
			this.inputTexts = inputTexts;
		}

		@Override
		public String call() throws Exception {
			String result = new String();
			try {
				GenerateContentConfig config = GenerateContentConfig.builder()
						.tools(List.of(Tool.builder().googleSearch(GoogleSearch.builder().build()).build())).build();
				List<Part> parts = new ArrayList<>();
				for (var inputText : inputTexts)
					parts.add(Part.builder().text(inputText).build());
				List<Content> contents = new ArrayList<>(Arrays.asList(Content.builder().parts(parts).build()));
				GenerateContentResponse response = client.models.generateContent("gemini-2.0-flash", contents, config);
				result = response.text();
			} catch (Exception e) {
				logger.error("Error while getting response for request {}", Arrays.toString(inputTexts), e);
				throw e;
			}
			return result;
		}
	}
}
