package in.foresthut.impact.species.eni;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;

public class GeminiResponseParser {
	public boolean isEndemic(String text) {
		text = text.replaceAll("(?<=.|!|\\?).+\\?", "");
		Pattern pattern = Pattern.compile("\\**\\bnot?\\b\\**[\\w\\s*(),\\\"'\\-&]+endemic",
				Pattern.CASE_INSENSITIVE & Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(text);
		return !text.contains("endemic") ? false : !matcher.find();
	}

	public boolean isNative(String text) {
		if (!isEndemic(text)) {
			text = text.replaceAll("(?<=.|!|\\?).+\\?", "");
			Pattern pattern = Pattern.compile("\\**\\bnot?\\b\\**[\\w\\s*(),\\\"'\\-&]+native",
					Pattern.CASE_INSENSITIVE & Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(text);
			return !text.contains("native") ? false : !matcher.find();
		} else {
			return true;
		}
	}

	public boolean isInvasive(String text) {
		text = text.replaceAll("(?<=.|!|\\?).+\\?", "");
		Pattern pattern = Pattern.compile("\\**\\bnot?\\b\\**[\\w\\s*(),\\\"'\\-&]+invasive",
				Pattern.CASE_INSENSITIVE & Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(text);
		return !text.contains("invasive") ? false : !matcher.find();
	}

	private boolean endemic, isNative, invasive;

	public void nlp(String text) {
	}

	public boolean endemic() {
		return endemic;
	}

	public boolean isNative() {
		return isNative;
	}

	public boolean invasive() {
		return invasive;
	}

}
