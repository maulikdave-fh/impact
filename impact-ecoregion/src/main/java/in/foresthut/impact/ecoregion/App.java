package in.foresthut.impact.ecoregion;

import java.io.IOException;

import in.foresthut.impact.ecoregion.service.EcoregionService;

public class App {

	//TODO : Why main is throwing IOException?
	public static void main(String[] args) throws IOException {
		EcoregionService.start();
	}
}

