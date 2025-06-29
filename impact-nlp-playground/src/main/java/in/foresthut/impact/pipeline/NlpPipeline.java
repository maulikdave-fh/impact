package in.foresthut.impact.pipeline;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class NlpPipeline {
	private static StanfordCoreNLP nlp;
	private static Properties props;
	private static String propertyNames = "";

	static {
		props = new Properties();
		// props.put("sentiment.model", "./model/species_attributes_model.ser.gz");
		propertyNames = "tokenize, ssplit, pos, lemma, ner, truecase, regexner, parse, depparse, coref";
		props.setProperty("annotators", propertyNames);
		props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");
//		props.setProperty("ner.model",
//				// "model/ner/ner-model.ser.gz,"
//				"edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz,"
//						+ "edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz,"
//						+ "edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz");
		props.setProperty("ner.model", "");
		props.setProperty("ner.fine.regexner.mapping", "./data/ner/regexner_rules.txt");
		//props.setProperty("ner.fine.regexner.mapping", "http://localhost:8081/prompt?text1=hello world");
		props.setProperty("ner.fine.regexner.ignorecase", "true");
	}

	private NlpPipeline() {

	}

	public static StanfordCoreNLP getInstance() {
		if (nlp == null) {
			nlp = new StanfordCoreNLP(props);
		}
		return nlp;
	}

}
