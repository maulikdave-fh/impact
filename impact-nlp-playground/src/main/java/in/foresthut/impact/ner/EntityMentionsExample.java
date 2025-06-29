package in.foresthut.impact.ner;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.util.*;

import java.util.*;

public class EntityMentionsExample {

	public static void main(String[] args) {
		Annotation document = new Annotation(
				"Psittacula columboides is endemic to the Western Ghats. It is also know as Malabar parakeet");
		Properties props = new Properties();
		props.setProperty("ner.model",
				"./model/ner/ner-model.ser.gz,edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz,edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz,edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz");

		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,entitymentions");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		pipeline.annotate(document);

		for (CoreMap entityMention : document.get(CoreAnnotations.MentionsAnnotation.class)) {
			System.out.println(entityMention);
			System.out.println(entityMention.get(CoreAnnotations.TextAnnotation.class));
		}
	}
}