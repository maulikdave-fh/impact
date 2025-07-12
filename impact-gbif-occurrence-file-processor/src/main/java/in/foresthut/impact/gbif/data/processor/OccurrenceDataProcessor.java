package in.foresthut.impact.gbif.data.processor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import in.foresthut.impact.config.Config;
import in.foresthut.impact.gbif.data.processor.infa.EcoregionClient;
import in.foresthut.impact.gbif.data.processor.infa.OccurrenceClient;

public class OccurrenceDataProcessor implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(OccurrenceDataProcessor.class);
	private final static Config config = Config.getInstance();
	private final static String EXCHNAGE_NAME = config.get("rabbitmq.exchange.name");
	private final static String ROUTING_KEY = "fromFile";
	private String occurrenceRawLine;
	private EcoregionClient ecoregionClient;
	private OccurrenceClient occurrenceClient;

	public OccurrenceDataProcessor(String occurrenceRaw, EcoregionClient ecoregionClient,
			OccurrenceClient occurrenceClient) {
		this.occurrenceRawLine = occurrenceRaw;
		this.ecoregionClient = ecoregionClient;
		this.occurrenceClient = occurrenceClient;
	}

	@Override
	public void run() {
		String[] fields = occurrenceRawLine.split("\t");
		// Extract necessary fields from the data to create Observation object
		Occurrence occurrence = toOccurrence(fields);

		// Fetch ecoregionId for the coordinates of the observation
		if (occurrence != null) {
			var ecoregion = ecoregionClient.getEcoregion(occurrence.decimalLatitude(), occurrence.decimalLongitude());
			// If valid ecoregion found, check with Observation service if gbifKey already
			// exists
			if (ecoregion != null) {
				boolean occurrenceExists = occurrenceClient.exists(ecoregion.bioregionName(), occurrence.key());
				if (!occurrenceExists)
					try {
						publishOccurrence(new OccurrenceMessage(ecoregion.id(), ecoregion.bioregionName(), occurrence));
					} catch (IOException | TimeoutException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}

		}

	}

	private void publishOccurrence(OccurrenceMessage occurrence) throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(config.get("rabbitmq.exchange.host"));
		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
			channel.exchangeDeclare(EXCHNAGE_NAME, BuiltinExchangeType.DIRECT, true);
			var message = new ObjectMapper().writeValueAsBytes(occurrence);
			channel.basicPublish(EXCHNAGE_NAME, ROUTING_KEY, MessageProperties.PERSISTENT_TEXT_PLAIN, message);
			// logger.info(" [x] Sent for GBIF observation id '{}'",
			// observation.observation().key());
		} catch (IOException e) {
			logger.error("{}", e);
			throw e;
		} catch (TimeoutException e1) {
			logger.error("{}", e1);
			throw e1;
		}
	}

	private static Occurrence toOccurrence(String[] fields) {
		try {
			if (!isValid(fields[182]))
				fields[182] = "0";

			if (fields.length > 222 && validData(fields)) {
				return new Occurrence(Long.valueOf(fields[0]), fields[201], fields[156], fields[157], fields[158],
						fields[159], fields[160], fields[161], fields[162], fields[163], fields[164], fields[165],
						fields[167], fields[222], Double.valueOf(fields[97]), Double.valueOf(fields[98]),
						Double.valueOf(fields[182]), fields[62], fields[5], fields[15], fields[24]);
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

	private static boolean validData(String[] fields) {
		return isValid(fields[15]) && isValid(fields[201]) && isValid(fields[97]) && isValid(fields[98])
				&& isValid(fields[62]);
	}

	private static boolean isValid(String str) {
		return str != null && !str.isBlank();
	}

	static record OccurrenceMessage(String ecoregionId, String regionName, Occurrence occurrence) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static record Occurrence(long key, String species, String kingdom, String phylum,
			@JsonProperty(value = "class") String _class, String order, String superFamily, String family,
			String subFamily, String tribe, String subTribe, String genus, String subGenus, String iucnRedListCategory,
			double decimalLatitude, double decimalLongitude, double elevation, String eventDate, String modified,
			String datasetName, String recordedBy) {
	}
}
