package in.foresthut.impact.gbif.data.processor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.gbif.data.processor.infa.EcoregionClient;
import in.foresthut.impact.gbif.data.processor.infa.OccurrenceClient;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private final static Config config = Config.getInstance();
	private final static EcoregionClient ecoregionClient = EcoregionClient.getInstance();
	private final static OccurrenceClient occurrenceClient = OccurrenceClient.getInstance();

	public static void main(String[] args) {
		logger.info("Started occurrence file processor!");
		String filePath = config.get("gbif-occurrences-filepath");
		var executor = Executors.newFixedThreadPool(6);

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			boolean head = true;
			while ((line = reader.readLine()) != null) {
				if (head) {
					head = false;
					continue;
				}
				executor.execute(new OccurrenceDataProcessor(line, ecoregionClient, occurrenceClient));
			}
			executor.shutdown();
		} catch (IOException e) {
			logger.error("Error reading file {}", filePath, e);
		}

	}
}
