package in.foresthut.impact.habitat.finder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private static final int PORT_NUMBER = 8083;

	public static void main(String[] args) {
		HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress(PORT_NUMBER), 0);
		} catch (IOException e) {
			logger.error("Error starting the server on port {}", PORT_NUMBER, e);
		}

		server.createContext("/iucn-habitats", new IUCNHabitatScoresHandler());
		Executor executor = Executors.newVirtualThreadPerTaskExecutor();
		server.setExecutor(executor);
		server.start();
		logger.info("Server is listening on port {}.", PORT_NUMBER);
	}
}
