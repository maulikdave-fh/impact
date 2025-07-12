package in.foresthut.impact.occurrence;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.foresthut.impact.occurrence.services.OccurrenceQueueMessageHandler;
import in.foresthut.impact.occurrence.services.OccurrenceService;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		// Start message handler service
		OccurrenceQueueMessageHandler.start();

		// Start a webservice
		OccurrenceService.start();
		
		logger.info("Occurrence Services started successfully.");
	}
}
