package in.foresthut.impact.habitat.finder.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import in.foresthut.impact.habitat.finder.daos.Elevation;
import in.foresthut.impact.habitat.finder.daos.IUCNHabitat;
import in.foresthut.impact.habitat.finder.daos.IUCNHabitatResponse;
import in.foresthut.impact.habitat.finder.daos.IUCNHabitatScore;
import in.foresthut.impact.habitat.finder.repo.IUCNHabitatsRepo;

public class IUCNHabitatFinder {
	private static IUCNHabitatFinder instance;
	private static IUCNHabitatsRepo repo;
	private static StanfordCoreNLP nlp;

	private static final List<String> STOP_WORDS = new ArrayList<>(Arrays.asList("is", "a", "an", "the", "and", "with",
			"between", "to", "are", "be", "they", "be", "in", "of", "from", "at", "that", "this"));

	private IUCNHabitatFinder() {
		repo = IUCNHabitatsRepo.getInstance();
		nlp = NlpPipeline.getInstance();
	}

	public static IUCNHabitatFinder getInstance() {
		if (instance == null)
			instance = new IUCNHabitatFinder();
		return instance;
	}

	public IUCNHabitatResponse calculateTfIdfScore(String text) {
		var iucnHabitats = repo.getAll();
		var words = tokens(text);

		Map<Double, IUCNHabitat> corpus = new HashMap<>();
		for (var iucnHabitat : iucnHabitats) {
			corpus.put(iucnHabitat.key(), iucnHabitat);
		}

		TfIdf tfIdfCalculator = new TfIdf();

		Map<Double, Double> scores = new HashMap<>();
		List<List<String>> keywordsCorpus = new ArrayList<>();
		
		for (var key : corpus.keySet())
			keywordsCorpus.add(corpus.get(key).keywords());
		
		for (var key : corpus.keySet()) {
			for (var word : words) {
				double score = tfIdfCalculator.calculateTfIdf(corpus.get(key).keywords(), keywordsCorpus, word);
				if (scores.containsKey(key)) {
					double currentScore = scores.get(key);
					scores.put(key, currentScore + score);
				} else {
					scores.put(key, score);
				}				
			}
		}
		
		
		Map<Double, IUCNHabitatScore> result = new HashMap<>();
		for (var key : scores.keySet()) {
			if (scores.get(key) != 0)
				result.put(key, new IUCNHabitatScore(key, corpus.get(key).name(), scores.get(key)));
		}
		var elevations = elevations(text);
		return new IUCNHabitatResponse(result, new Elevation(elevations[0]), new Elevation(elevations[1]));
	}

	public IUCNHabitatResponse calculate(String text) {
		var elevations = elevations(text);
		var iucnHabitats = repo.getAll();
		var tokens = tokens(text);

		Map<Double, IUCNHabitatScore> result = new HashMap<>();

		for (var token : tokens) {
			for (var iucnHabitat : iucnHabitats) {
				if (iucnHabitat.keywords().contains(token)) {
					var key = iucnHabitat.key();
					if (result.containsKey(key)) {
						var score = result.get(key).score();
						result.put(key, new IUCNHabitatScore(key, iucnHabitat.name(), ++score));
					} else {
						result.put(key, new IUCNHabitatScore(key, iucnHabitat.name(), 1));
					}
				}
			}
		}

		return new IUCNHabitatResponse(result, new Elevation(elevations[0]), new Elevation(elevations[1]));
	}

	private int[] elevations(String text) {
		Pattern pattern = Pattern.compile("(?<!\\.|\\d)\\d{1,4}(?=.+meters|mt|mtrs)");
		Matcher matcher = pattern.matcher(text.replaceAll(",", ""));

		List<Integer> matches = new ArrayList<>();
		while (matcher.find()) {
			var match = matcher.group();
			if (!match.isBlank()) {
				matches.add(Integer.valueOf(match));
			}
		}

		if (matches.size() == 0) {
			return new int[2];
		} else if (matches.size() == 1) {
			if (text.contains("above " + matches.get(0))) {
				return new int[] { Integer.valueOf(matches.get(0)), 0 };
			} else {
				return new int[] { 0, Integer.valueOf(matches.get(0)) };
			}
		} else {
			Collections.sort(matches);
			return new int[] { matches.get(0), matches.get(matches.size() - 1) };
		}
	}

	private List<String> tokens(String text) {
		String cleanText = clean(text);
		CoreDocument doc = new CoreDocument(cleanText);
		nlp.annotate(doc);
		List<CoreSentence> sentences = doc.sentences();

		List<String> tokens = new ArrayList<>();
		for (var sentence : sentences) {
			for (var token : sentence.tokens()) {
				if (!token.tag().equals("VBD") && !token.tag().equals("VBN") && !token.tag().equals("NNP"))
					if (token.ner() == null || token.ner().equals("O")) {
						if (!STOP_WORDS.contains(token.word().toLowerCase())) {
							tokens.add(token.lemma().toLowerCase());
						}
					}
			}
		}
		return tokens;
	}

	private String clean(String text) {
		String cleanText = text.replaceAll("\\*", "").replaceAll("\n", " ").replaceAll("\\.", "").replaceAll(",", "")
				.replaceAll(";", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\s{2,}", " ");
		return cleanText;
	}

}
