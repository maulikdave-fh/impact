package in.foresthut.impact.habitat.finder.daos;

import java.util.List;

public record IUCNHabitat(double key, String name, List<String> keywords, int lowerElevation,
		int upperElevation) {

}
