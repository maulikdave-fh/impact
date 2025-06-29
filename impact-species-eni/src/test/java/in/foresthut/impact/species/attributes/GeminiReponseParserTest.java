package in.foresthut.impact.species.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import in.foresthut.impact.species.eni.GeminiResponseParser;
import in.foresthut.impact.species.eni.infra.GeminiClient;

/**
 * Unit test for simple App.
 */
public class GeminiReponseParserTest {
	static GeminiResponseParser parser;
	static String greatHornbill, lantana, shieldtail, myna, gulmohar, bulbul;

	@BeforeAll
	static void setup() {
		parser = new GeminiResponseParser();
	}

	@ParameterizedTest
	@MethodSource("additionalArguments")
	void testAllUsingRegex(String speciesName, boolean endemic, boolean isNative, boolean invasive) {
		String[] prompts = new String[] { "is " + speciesName + " endemic to the Western Ghats?",
				"is " + speciesName + " native to the Western Ghats?", "is " + speciesName + " invasive in India?",
				speciesName + " habitats as per IUCN habitat classification scheme" };
		String response = GeminiClient.getInstance().send(prompts);

		boolean actualEndemic = parser.isEndemic(response);
		boolean actualNative = parser.isNative(response);
		boolean actualInvasive = parser.isInvasive(response);
		assertEquals(endemic, actualEndemic, "For endemic, expected " + endemic + ", received " + actualEndemic);
		assertEquals(isNative, actualNative, "For native, expected " + isNative + ", received " + actualNative);
		assertEquals(invasive, actualInvasive, "For invasive, expected " + invasive + ", received " + actualInvasive);
	}
	
//	@ParameterizedTest
//	@MethodSource("additionalArguments")
	void testAllUsingNLP(String speciesName, boolean endemic, boolean isNative, boolean invasive) {
		String[] prompts = new String[] { "is " + speciesName + " endemic to the Western Ghats?",
				"is " + speciesName + " native to the Western Ghats?", "is " + speciesName + " invasive in India?",
				speciesName + " habitats as per IUCN habitat classification scheme" };
		String response = GeminiClient.getInstance().send(prompts);

		parser.nlp(response);
		boolean actualEndemic = parser.endemic();
		boolean actualNative = parser.isNative();
		boolean actualInvasive = parser.invasive();
		assertEquals(endemic, actualEndemic, "For endemic, expected " + endemic + ", received " + actualEndemic);
		assertEquals(isNative, actualNative, "For native, expected " + isNative + ", received " + actualNative);
		assertEquals(invasive, actualInvasive, "For invasive, expected " + invasive + ", received " + actualInvasive);
	}

	private static Stream<Arguments> additionalArguments() {
		return Stream.of(Arguments.of("Buceros bicornis", false, true, false)
//				Arguments.of("Pycnonotus jocosus", false, true, false),
//				Arguments.of("Uropeltis phipsonii", true, true, false),
//				Arguments.of("Delonix regia", false, false, false),
//				Arguments.of("Acridotheres tristis", false, true, true),
//				Arguments.of("Pangora matherana", false, true, false)
				);
	}

}
