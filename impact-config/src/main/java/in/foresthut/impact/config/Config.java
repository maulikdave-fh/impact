package in.foresthut.impact.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;

/**
 * Singleton that loads properties from config.properties.
 * 
 * @author maulik-dave
 */
public class Config {
	private static Config instance;
	private final static String CONFIG_FILE = "config.properties";
	private final static Logger logger = LoggerFactory.getLogger(Config.class);

	private final Properties properties;

	private Config() {
		this.properties = new Properties();

		try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
			if (input == null) {
				final String traceId = UUID.randomUUID().toString();
				logger.error("{} Config file {}  not found.", traceId, CONFIG_FILE);
				throw new ConfigFileNotFoundException(String.format("Config file %s not found", CONFIG_FILE), traceId,
						null);
			}
			properties.load(input);
			logger.info("Configurations loaded successfully.");
		} catch (IOException ex) {
			final String traceId = UUID.randomUUID().toString();
			logger.error("{} Could not load properties from {}", traceId, CONFIG_FILE, ex);
			throw new ConfigLoadException(String.format("Could not load properties from %s", CONFIG_FILE), traceId, ex);
		}
	}

	/**
	 * Returns a singleton instance of Config.
	 */
	public static synchronized Config getInstance() {
		if (instance == null) {
			instance = new Config();
		}
		return instance;
	}

	/**
	 * Returns a property value for a given key
	 * 
	 * @param key as String
	 * @return property value as String
	 */
	public String get(String key) {
		var value = properties.getProperty(key);
		if (value == null) {
			logger.error("Property with key {} not found. Check your config.properties", key);
			throw new AlreadyLoggedException("Property with key " + key + " not found.", null, null);
		}
		return value;
	}
}
