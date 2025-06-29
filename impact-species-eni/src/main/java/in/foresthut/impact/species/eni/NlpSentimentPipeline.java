package in.foresthut.impact.species.eni;

import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class NlpSentimentPipeline {

	private static StanfordCoreNLP pipeline;
	private static NlpSentimentPipeline nlp;
	private static Properties props;
	private static String propertyNames = "";

	static {
		props = new Properties();
		props.put("sentiment.model", "./model/sentiment/species_eni_model.ser.gz");
		propertyNames = "tokenize, ssplit, pos, lemma, ner, parse, sentiment";
		props.setProperty("annotators", propertyNames);
		props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");
	}

	private NlpSentimentPipeline() {
		pipeline = new StanfordCoreNLP(props);
	}

	public static NlpSentimentPipeline getInstance() {
		if (nlp == null)
			nlp = new NlpSentimentPipeline();
		return nlp;
	}

	public void init() {
		Properties props = new Properties();
		props.put("sentiment.model", "./model/sentiment/species_eni_model.ser.gz");
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, sentiment");
		pipeline = new StanfordCoreNLP(props);
	}

	public static void findIntent(String text) {
		edu.stanford.nlp.pipeline.Annotation annotation = pipeline.process(text);
		for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
			SemanticGraph sg = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
			String intent = "It does not seem that the sentence expresses an explicit intent.";
			for (SemanticGraphEdge edge : sg.edgeIterable()) {
				if (edge.getRelation().getLongName() == "direct object") {
					String tverb = edge.getGovernor().originalText();
					String dobj = edge.getDependent().originalText();
					dobj = dobj.substring(0, 1).toUpperCase() + dobj.substring(1).toLowerCase();
					intent = tverb + dobj;
				}
			}
			System.out.println("Sentence:\t" + sentence);
			System.out.println("Intent:\t\t" + intent + "\n");
		}
	}

	public int estimateSentiment(String text) {
		int sentimentInt = 1;
		String sentimentName;
		edu.stanford.nlp.pipeline.Annotation annotation = pipeline.process(text);
		for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				Tree tree = sentence.get(SentimentAnnotatedTree.class);
				sentimentInt = RNNCoreAnnotations.getPredictedClass(tree);
				sentimentName = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
				System.out.println(sentimentName + "\t" + sentimentInt + "\t" + sentence);
		}
		return sentimentInt == 2 ? 1 : sentimentInt;
	}

	public static void main(String[] args) {
		// String text = "I have posted five stars.";
		// String text = "This is an excellent book. I enjoy reading it. I can read on
		// Sundays. Today is only Tuesday. Can't wait for next Sunday. The working week
		// is unbearably long. It's awful.";
		String text = "Canarium strictum is not endemic to the Western Ghats.";
		text = text.replaceAll("\\*", "");
		NlpSentimentPipeline nlp = new NlpSentimentPipeline();
		nlp.init();
		nlp.estimateSentiment(text);
	}

}
