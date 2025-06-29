package in.foresthut.impact.gemini.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;

public class GoogleGeminiConnector {
	private static final Logger logger = LoggerFactory.getLogger(GoogleGeminiConnector.class);
	private static final int PORT_NUMBER = 8081;

	private GoogleGeminiConnector() {

	}

	public static void start() {
		HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress(PORT_NUMBER), 0);
		} catch (IOException e) {
			logger.error("Error starting the server on port {}", PORT_NUMBER, e);
		}

		server.createContext("/prompt", new PromptHandler());
		server.createContext("/status", new StatusHandler());
		Executor executor = Executors.newVirtualThreadPerTaskExecutor();
		server.setExecutor(executor);
		server.start();
		logger.info("Server is listening on port {}.", PORT_NUMBER);
	}

	static void sendResponse(Object responseObj, HttpExchange exchange, int httpStatusCode, String uuid)
			throws IOException {
		String jsonString = new ObjectMapper().writeValueAsString(responseObj);
		byte[] response = jsonString.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		if (uuid != null)
			exchange.getResponseHeaders().set("Error-Trace-Id", uuid);
		exchange.sendResponseHeaders(httpStatusCode, response.length);
		OutputStream outStream = exchange.getResponseBody();
		outStream.write(response);
		outStream.close();
	}

	private static class PromptHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (exchange.getRequestMethod().equals("GET")) {
				String[] queryStringPairs = exchange.getRequestURI().getQuery().split("&");
				
				if (queryStringPairs.length == 0) {
					final String traceId = UUID.randomUUID().toString();
					logger.error("[Error-trace-id] {} Empty querystring", traceId, null);
					sendResponse("Invalid Request. Check for typos.", exchange, 404, traceId);
					return;
				}
				
				String[] queryTexts = Stream.of(queryStringPairs).map(pair -> pair.split("=")[1]).toArray(String[]::new);
				
//				if (!queryString[0].equals("text")) {
//					final String traceId = UUID.randomUUID().toString();
//					logger.error("[Error-trace-id] {} Invalid querystring key '{}'", traceId, queryString[0]);
//					sendResponse("Invalid Request. Check for typos.", exchange, 404, traceId);
//					return;
//				}

				try {
					var response = GoogleGeminiClient.getInstance().send(queryTexts, true);
					sendResponse(response, exchange, 200, null);
				} catch (AlreadyLoggedException ex) {
					sendResponse("Call limit exceeded for Google Gemini. Try tomorrow.", exchange, 500, ex.traceId());
				}
			} else {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid request method {}", traceId, exchange.getRequestMethod());
				sendResponse("Invalid Request Method.", exchange, 405, traceId);
			}
		}
	}

	private static class StatusHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (exchange.getRequestMethod().equals("GET")) {
				sendResponse("OK", exchange, 200, null);
			} else {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid request method {}", traceId, exchange.getRequestMethod());
				sendResponse("Invalid Request Method", exchange, 405, traceId);
			}
		}
	}
}
