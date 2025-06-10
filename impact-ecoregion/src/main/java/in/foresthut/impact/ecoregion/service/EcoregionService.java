package in.foresthut.impact.ecoregion.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import in.foresthut.impact.commons.AlreadyLoggedException;
import in.foresthut.impact.ecoregion.exceptions.InvalidEcoregionIdException;
import in.foresthut.impact.ecoregion.repo.Ecoregion;
import in.foresthut.impact.ecoregion.repo.Ecoregions;

public class EcoregionService {
	private static final Logger logger = LoggerFactory.getLogger(EcoregionService.class);
	private static final int PORT_NUMBER = 8080;

	private EcoregionService() {

	}

	public static void start() throws IOException {
		HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress(PORT_NUMBER), 0);
		} catch (IOException e) {
			logger.error("Error starting the server on port {}", PORT_NUMBER, e);
		}

		server.createContext("/ecoregions", new EcoregionsHandler());
		server.createContext("/ecoregion", new EcoregionSplitHandler());
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

	private static class EcoregionsHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (exchange.getRequestMethod().equals("GET")) {
				String[] path = exchange.getRequestURI().getPath().split("/");
				if (path.length > 2) {
					final String traceId = UUID.randomUUID().toString();
					logger.error("[Error-trace-id] {} Invalid path {}", traceId, exchange.getRequestURI().getPath());
					sendResponse("Invalid Request", exchange, 400, traceId);
					return;
				}
				try {
					List<Ecoregion> ecoregions = Ecoregions.get();
					sendResponse(ecoregions, exchange, 200, null);
				} catch (AlreadyLoggedException ex) {
					final String traceId = ex.traceId();
					sendResponse("Oops! This shouldn't have been happened.", exchange, 500, traceId);
				} catch (Exception ex) {
					final String traceId = UUID.randomUUID().toString();
					logger.error("[Error-trace-id] {} Unexpected error", traceId, ex);
					sendResponse("Oops! This shouldn't have been happened.", exchange, 500, traceId);
				}
			} else {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid request method {}", traceId, exchange.getRequestMethod());
				sendResponse("Invalid Request Method", exchange, 405, traceId);
			}
		}
	}

	private static class EcoregionSplitHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (exchange.getRequestMethod().equals("GET")) {
				String[] path = exchange.getRequestURI().getPath().split("/");
				if (path.length != 4) {
					final String traceId = UUID.randomUUID().toString();
					logger.error("[Error-trace-id] {} Invalid Request '{}'", traceId,
							exchange.getRequestURI().getPath());
					sendResponse("Invalid Request. Check for typos.", exchange, 404, traceId);
					return;
				}
				String id = path[2];

				String action = path[3];
				if (!action.equals("split")) {
					final String traceId = UUID.randomUUID().toString();
					logger.error("[Error-trace-id] {} Invalid Action in Request '/{}'. Check for typo.", traceId,
							action);
					sendResponse("Invalid Request. Check for typos.", exchange, 404, traceId);
					return;
				}

				List<String> splits = new ArrayList<>();
				try {
					splits = Ecoregion.split(id);
					sendResponse(splits, exchange, 200, null);
				} catch (InvalidEcoregionIdException e) {
					final String traceId = e.traceId();
					sendResponse("Invalid ecoregion id.", exchange, 404, traceId);
				} catch (AlreadyLoggedException ex) {
					sendResponse("Oops! This shouldn't have been happened.", exchange, 500, ex.traceId());
				} catch (Exception ex) {
					final String traceId = UUID.randomUUID().toString();
					logger.error("[Error-trace-id] {} Unexpected error", traceId, ex);
					sendResponse("Oops! This shouldn't have been happened.", exchange, 500, traceId);
				}
			} else {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid request method {}", traceId, exchange.getRequestMethod());
				sendResponse("Invalid Request Method.", exchange, 405, traceId);
			}
		}
	}
}
