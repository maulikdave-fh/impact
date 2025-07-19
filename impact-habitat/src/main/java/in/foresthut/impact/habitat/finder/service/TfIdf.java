package in.foresthut.impact.habitat.finder.service;

import java.util.List;

public class TfIdf {
	public static double calculateTermFrequency(List<String> words, String term) {
		long count = 0;
		for (String word : words) {
			if (term.equalsIgnoreCase(word)) {
				count++;
			}
		}

		double termFrequency = (double) count / words.size();
		return termFrequency;
	}
	
	public double calculateInverseDocumentFrequency(List<List<String>> corpus, String term) {
	    double documentCountWithTerm = 0;
	    for (List<String> document : corpus) {
	        if (document.contains(term.toLowerCase())) { // Case-insensitive check
	            documentCountWithTerm++;
	        }
	    }
	    if (documentCountWithTerm == 0) {
	        return 0; // Avoid division by zero
	    }
	    return Math.log(corpus.size() / documentCountWithTerm);
	}
	
	public double calculateTfIdf(List<String> document, List<List<String>> corpus, String term) {
	    double tf = calculateTermFrequency(document, term);
	    double idf = calculateInverseDocumentFrequency(corpus, term);
	    return tf * idf;
	}

}
