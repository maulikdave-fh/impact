package in.foresthut.impact.species.eni;

import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class NlpSentenceSplitterPipeline {
	private static StanfordCoreNLP nlpSS;
	private static Properties props;
	private static String propertyNames = "";

	static {
		props = new Properties();
		propertyNames = "tokenize, ssplit";
		props.setProperty("annotators", propertyNames);
		props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");
	}

	private NlpSentenceSplitterPipeline() {
	}

	public static StanfordCoreNLP getInstance() {
		if (nlpSS == null)
			nlpSS = new StanfordCoreNLP(props);
		return nlpSS;
	}
}
