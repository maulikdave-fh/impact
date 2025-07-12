package in.foresthut.impact.ecoregion.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

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
		server.createContext("/region", new CombineEcoregionsMapHandler());
		server.createContext("/ecoregionid/for", new EcoregionForCoordinatesHandler());
		Executor executor = Executors.newVirtualThreadPerTaskExecutor();
		server.setExecutor(executor);
		server.start();
		logger.info("Server is listening on port {}.", PORT_NUMBER);
	}

}
