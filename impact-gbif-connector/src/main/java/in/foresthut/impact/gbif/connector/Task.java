package in.foresthut.impact.gbif.connector;

public record Task(String ecoregionId, String regionName, String polygon, String dateFrom, String dateTo, int messageHash) {
}