package in.foresthut.impact.ner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.SeqClassifierFlags;

public class CustomNerModelGenerator {
	private final static Logger logger = LoggerFactory.getLogger(CustomNerModelGenerator.class);

	private void trainAndWrite(String prop, String trainingFilepath) {
		Properties props = load(prop);
		//props.setProperty("serializeTo", modelOutPath); // if input use that, else use from properties file.
		if (trainingFilepath != null) {
			props.setProperty("trainFile", trainingFilepath);
		}
		SeqClassifierFlags flags = new SeqClassifierFlags(props);
		CRFClassifier<CoreLabel> crf = new CRFClassifier<>(flags);
		crf.train();
		crf.serializeClassifier(props.getProperty("serializeTo"));
	}

	private Properties load(String nerConfigFile) {
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(nerConfigFile)) {
			if (input == null) {
				logger.error("NER config file {} not found.", nerConfigFile);
				throw new FileNotFoundException(String.format("NER Config file %s not found"));
			}
			logger.info("Configurations loaded successfully.");
			Properties properties = new Properties();
			properties.load(input);
			return properties;
		} catch (IOException ex) {
			logger.error("Could not load properties from {}", nerConfigFile, ex);
			throw new RuntimeException(String.format("Could not load properties from %s", nerConfigFile), ex);
		}
	}

	public static void main(String[] args) {
		new CustomNerModelGenerator().trainAndWrite("ner.properties", null);
	}
}
