package in.foresthut.impact.ner;

import java.util.List;
import java.util.Map;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;
import in.foresthut.impact.pipeline.NlpPipeline;

public class CustomNerModelUtil {

	public static void main(String[] args) {
		String[] tests = new String[] {
				"Yes, Psittacula columboides is endemic to the western Ghats and western penninsular India. It is also know as Malabar parakeet."};
//				"Blue-winged parakeet is another name for it in southern INDIA. It is found in North Western Ghats Moist Deciduous Forest" };
		var nlp = NlpPipeline.getInstance();

		for (String item : tests) {
//			Annotation document = new Annotation(item);
//			nlp.annotate(document);
//			System.out.println("---");
//			System.out.println("coref chains");
//			for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
//				System.out.println("\t" + cc);
//			}
//			for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
//				System.out.println("---");
//				System.out.println("mentions");
//				for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
//					System.out.println("\t" + m);
//				}
//			}

			CoreDocument doc = new CoreDocument(item);
			nlp.annotate(doc);
			List<CoreSentence> sentences = doc.sentences();
			for (var sentence : sentences) {
				System.out.println("Original sentence: ");
				System.out.println(sentence.text() + "\n");
				System.out.println("Example: Named Entity Recognization");
				for (var token : sentence.tokens()) {
					System.out.print(token.originalText() + "/" + token.ner() + " ");
				}
				System.out.println("\n");

				Tree constituencyParse = sentence.constituencyParse();
				System.out.println("Example: constituency parse");
				System.out.println(constituencyParse);
				System.out.println();

				// dependency parse for the second sentence
				SemanticGraph dependencyParse = sentence.dependencyParse();
				System.out.println("Example: dependency parse");
				System.out.println(dependencyParse);
				System.out.println();
			}

			System.out.println("Example: Entity mentions");
			for (var entities : doc.entityMentions()) {
				System.out.print(entities + ", ");
			}
			System.out.println("\n");

			// get document wide coref info
			Map<Integer, CorefChain> corefChains = doc.corefChains();
			System.out.println("Example: coref chains for document");
			System.out.println(corefChains);
		}
		System.out.println("------------------------------");

	}
}
