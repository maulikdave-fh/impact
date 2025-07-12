package in.foresthut.impact.occurrence.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

public class OccurrenceService {
	private static final Logger logger = LoggerFactory.getLogger(OccurrenceService.class);
	private static final int PORT_NUMBER = 8082;

	private OccurrenceService() {

	}

	public static void start() throws IOException {
		HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress(PORT_NUMBER), 0);
		} catch (IOException e) {
			logger.error("Error starting the server on port {}", PORT_NUMBER, e);
		}

		server.createContext("/occurrences", new OccurrenceExistsHandler());
		server.createContext("/modifiedOn", new OccurrenceLastModifiedOnForEcoregionHandler());
		Executor executor = Executors.newVirtualThreadPerTaskExecutor();
		server.setExecutor(executor);
		server.start();
		logger.info("Server is listening on port {}.", PORT_NUMBER);
	}
}
