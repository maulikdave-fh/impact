package in.foresthut.impact;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
	private static Config instance;
	private final static String CONFIG_FILE = "config.properties";
	private final static Logger logger = LoggerFactory.getLogger(Config.class);

	private final Properties properties;

	private Config() {
		this.properties = new Properties();

		try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
			if (input == null)
				logger.error("Config file {}  not found.", CONFIG_FILE);
			properties.load(input);
			logger.info("Configurations loaded successfully.");
		} catch (IOException ex) {
			logger.error("Could not load properties from {}", CONFIG_FILE, ex);
		}
	}

	public static synchronized Config getInstance() {
		if (instance == null) {
			instance = new Config();
		}
		return instance;
	}

	public String get(String key) {
		return properties.getProperty(key);
	}
}
