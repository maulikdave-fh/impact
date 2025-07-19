package in.foresthut.impact.habitat.finder.daos;

import java.util.Map;

public record IUCNHabitatResponse(Map<Double, IUCNHabitatScore> scores, Elevation lowerElevation,
		Elevation upperElevation) {

}


