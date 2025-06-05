package in.foresthut.impact.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import in.foresthut.impact.exceptions.InvalidEcoregionIdException;
import in.foresthut.impact.repo.Ecoregion;
import in.foresthut.impact.repo.Ecoregions;

public class EcoregionService {
	private static final Logger logger = LoggerFactory.getLogger(EcoregionService.class);
	private static final int PORT_NUMBER = 8080;

	private EcoregionService() {

	}

	public static void start() {
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

	static void sendResponse(Object responseObj, HttpExchange exchange, int httpStatusCode) throws IOException {
		String jsonString = new ObjectMapper().writeValueAsString(responseObj);
		byte[] response = jsonString.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
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
					logger.error("Invalid path {}", exchange.getRequestURI().getPath());
					sendResponse("Invalid Request", exchange, 400);
					return;
				}
				try {
					List<Ecoregion> ecoregions = Ecoregions.get();
					sendResponse(ecoregions, exchange, 200);
				} catch (Exception ex) {
					logger.error("Unexpected error {}", ex);
					sendResponse("Unexpected Error.", exchange, 500);
				}
			} else {
				logger.error("Invalid request method {}" + exchange.getRequestMethod());
				sendResponse("Invalid Request Method", exchange, 405);
			}
		}
	}

	private static class EcoregionSplitHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (exchange.getRequestMethod().equals("GET")) {
				String[] path = exchange.getRequestURI().getPath().split("/");
				String id = path[2];

				List<String> splits = new ArrayList<>();
				try {
					splits = Ecoregion.split(id);

					sendResponse(splits, exchange, 200);
				} catch (InvalidEcoregionIdException e) {
					logger.error("Invalid ecoregion id {}" + id);
					sendResponse("Invalid ecoregion id.", exchange, 404);
				} catch (Exception ex) {
					logger.error("Unexpected error {}", ex);
					sendResponse("Unexpected Error.", exchange, 500);
				}
			} else {
				logger.error("Invalid request method {}" + exchange.getRequestMethod());
				sendResponse("Invalid Request Method.", exchange, 405);
			}
		}
	}
}