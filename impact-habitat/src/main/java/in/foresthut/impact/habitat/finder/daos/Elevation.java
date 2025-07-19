package in.foresthut.impact.habitat.finder.daos;

public record Elevation(int value, String unit) {
	public Elevation(int value) {
		this(value, "meters");
	}
}