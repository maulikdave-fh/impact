package in.foresthut.impact.habitat.finder.service;

public class IUCNHabitatFinderTest {
	public static void main(String[] args) {
		IUCNHabitatFinder.getInstance().calculateTfIdfScore(
				"White-rumped vultures (Gyps bengalensis) primarily inhabit open areas near villages, towns, and cities, often in lowlands but also ascending to 1500 meters in the Himalayan foothills. They are frequently found in grasslands, shrublands, and other plains, though they can also be seen in more hilly regions");
	}
}
