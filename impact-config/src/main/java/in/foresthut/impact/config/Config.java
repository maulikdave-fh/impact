package in.foresthut.impact.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
				logger.error("Config file {}  not found.", CONFIG_FILE);
				throw new ConfigFileNotFoundException(String.format("Config file %s not found", CONFIG_FILE));
			}
			properties.load(input);
			logger.info("Configurations loaded successfully.");
		} catch (IOException ex) {
			logger.error("Could not load properties from {}", CONFIG_FILE, ex);
			throw new ConfigLoadException(String.format("Could not load properties from %s", CONFIG_FILE), ex);
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
		return properties.getProperty(key);
	}
}
