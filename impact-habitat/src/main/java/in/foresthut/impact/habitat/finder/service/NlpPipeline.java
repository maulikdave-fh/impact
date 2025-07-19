package in.foresthut.impact.habitat.finder.service;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

class NlpPipeline {
	private static StanfordCoreNLP nlp;
	private static Properties props;
	private static String propertyNames = "";

	static {
		props = new Properties();
		propertyNames = "tokenize, pos, lemma, ner";
		props.setProperty("annotators", propertyNames);
		props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");
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
