package in.foresthut.impact.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

public class PolygonTest {

	@Test
	void testSplit_whenPolygonLargerThanMAXAREA_shouldReturnMultiplePolygons() {

		Coordinate[] shell = new Coordinate[] { new Coordinate(1, 1), new Coordinate(-1, 1), new Coordinate(-1, 0),
				new Coordinate(1, 0), new Coordinate(1, 1) };
		var polygon = new GeometryFactory().createPolygon(shell);
		Geometry multiPolygon = new GeometryFactory()
				.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] { polygon });

		try {
			Polygon rectangle = new Polygon(new GeoJsonWriter().write(multiPolygon));
			double expectedTotalArea = multiPolygon.getArea();
			List<String> actual = rectangle.split();

			double actualArea = 0;
			for (var p : actual) {
				var geometry = new WKTReader().read(p);
				assertTrue(geometry.isValid());
				assertEquals("Polygon", geometry.getGeometryType());
				assertTrue(geometry.getArea() <= Polygon.getMaxArea());
				actualArea += geometry.getArea();
			}

			assertEquals(expectedTotalArea, actualArea);
		} catch (InvalidGeoJsonException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	@Test
	void testSplit_whenPolygonSmallerThanMAXAREA_shouldReturnOriginalMultiPolygon() {

		Coordinate[] shell = new Coordinate[] { new Coordinate(0.001, 0.001), new Coordinate(-0.001, 0.001),
				new Coordinate(-0.001, 0), new Coordinate(0.001, 0), new Coordinate(0.001, 0.001) };
		var polygon = new GeometryFactory().createPolygon(shell);
		Geometry multiPolygon = new GeometryFactory()
				.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] { polygon });

		try {
			Polygon rectangle = new Polygon(new GeoJsonWriter().write(multiPolygon));
			double expectedTotalArea = multiPolygon.getArea();
			List<String> actual = rectangle.split();

			double actualArea = 0;
			for (var p : actual) {
				var geometry = new WKTReader().read(p);
				assertTrue(geometry.isValid());
				assertEquals("MultiPolygon", geometry.getGeometryType());
				assertTrue(geometry.getArea() <= Polygon.getMaxArea());
				actualArea += geometry.getArea();
			}

			assertEquals(expectedTotalArea, actualArea);
			assertEquals(1, actual.size());
		} catch (InvalidGeoJsonException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	@Test
	void testSplit_whenInvalidPolygon_shouldThrowInvalidGeoJsonException() {
		try {
			String geoJson = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[1,1],[-1,1],[-1,0.0],[1,0.0]]]]}";
			Polygon rectangle = new Polygon(geoJson);
			assertThrows(InvalidGeoJsonException.class, () -> {
				rectangle.split();
			});
		} catch (InvalidGeoJsonException e) {
		}
	}
}
