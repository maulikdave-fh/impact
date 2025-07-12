package in.foresthut.impact.occurrence.daos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Occurrence(long key, String species, String kingdom, String phylum,
		@JsonProperty(value = "class") String _class, String order, String superFamily, String family, String subFamily,
		String tribe, String subTribe, String genus, String subGenus, String iucnRedListCategory,
		double decimalLatitude, double decimalLongitude, double elevation, String eventDate, String modified,
		String datasetName, String recordedBy) {
}