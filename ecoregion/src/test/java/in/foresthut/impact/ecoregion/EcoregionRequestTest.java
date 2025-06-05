package in.foresthut.impact.ecoregion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import in.foresthut.impact.ecoregion.entities.Ecoregion;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class EcoregionRequestTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void ecoregionShouldReturn254Ecoregion() throws Exception {
		assertEquals(254,
				this.restTemplate
						.getForObject("http://localhost:" + port + "/ecoregions/18.266449523603537/73.49784175801707",
								Ecoregion.class)
						.getRegionId());
	}

	@Test
	void ecoregionShouldReturn404() throws Exception {
		assertEquals(HttpStatus.NOT_FOUND, this.restTemplate
				.getForEntity("http://localhost:" + port + "/ecoregions/8/65", Ecoregion.class).getStatusCode());
	}

}
