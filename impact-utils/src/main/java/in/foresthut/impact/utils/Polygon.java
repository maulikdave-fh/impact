package in.foresthut.impact.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An utility class for Polygon.
 *
 * @author maulik-dave
 */

public class Polygon {
	private static final double MAX_AREA = 0.05d;
	private final static Logger logger = LoggerFactory.getLogger(Polygon.class);

	private Geometry poly;

	/**
	 * Expects a valid geoJSON polygon or multipolygon string.
	 * 
	 * @param geoJsonPolygon
	 * @throws InvalidGeoJsonException if invalid geoJSON string
	 */
	public Polygon(String geoJsonPolygon) {
		try {
			this.poly = new GeoJsonReader().read(geoJsonPolygon);
		} catch (ParseException ex) {
			final String traceId = UUID.randomUUID().toString();
			logger.error("[Error-trace-id] {} Invalid geoJSON {}", traceId, geoJsonPolygon, ex);
			throw new InvalidGeoJsonException("Invalid geoJSON: ", traceId, ex);
		}
	}

	/**
	 * For point in polygon check.
	 * 
	 * @param latitude
	 * @param longitude
	 * @return true if the coordinate is falls inside the polygon
	 */
	public boolean contains(double latitude, double longitude) {
		Coordinate point = new Coordinate(longitude, latitude, 0);
		Geometry location = new GeometryFactory().createPoint(point);
		return this.poly.contains(location);
	}

	/**
	 * Splits the polygon into multiple polygons. Each smaller polygon will be no
	 * greater than MAX_AREA
	 * 
	 * @return list of polygons in WKT format
	 */
	public List<String> split() {
		List<Geometry> polys = split(poly);
		return polys.stream().map(p -> p.toText()).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return poly.toString();
	}

	private List<Geometry> split(Geometry inputPoly) {
		List<Geometry> result = new ArrayList<>();
		if (inputPoly.getArea() <= MAX_AREA) {
			// GBIF needs CCW (Counter ClockWise) polygon.
			// Make polygon CCW before adding
			result.add(inputPoly.reverse());
			return result;
		}
		Geometry nodedLinework = inputPoly.getBoundary().union(bisector(inputPoly));
		Geometry[] polys = polygonize(nodedLinework);

		// Only keep polygons which are inside the input
		List<Geometry> output = new ArrayList<>();
		for (Geometry poly : polys) {
			if (inputPoly.contains(poly.getInteriorPoint())) {
				output.add(poly);
			}
		}

		for (var poly : output) {
			result.addAll(split(poly));
		}
		return result;
	}

	private Geometry bisector(Geometry targetPolygon) {
		Point center = targetPolygon.getCentroid();
		double width = targetPolygon.getEnvelopeInternal().getWidth();
		double height = targetPolygon.getEnvelopeInternal().getHeight();
		Coordinate c0 = null;
		Coordinate c1 = null;
		if (width >= height) {
			c0 = new Coordinate(center.getX(), targetPolygon.getEnvelopeInternal().getMaxY());
			c1 = new Coordinate(center.getX(), targetPolygon.getEnvelopeInternal().getMinY());
		} else {
			c0 = new Coordinate(targetPolygon.getEnvelopeInternal().getMaxX(), center.getY());
			c1 = new Coordinate(targetPolygon.getEnvelopeInternal().getMinX(), center.getY());
		}
		Geometry bisector = new GeometryFactory().createLineString(new Coordinate[] { c0, c1 });
		return bisector;
	}

	private Geometry[] polygonize(Geometry geometry) {
		List<?> lines = LineStringExtracter.getLines(geometry);
		Polygonizer polygonizer = new Polygonizer();
		polygonizer.add(lines);
		Collection<?> polys = polygonizer.getPolygons();
		Geometry[] polyArray = GeometryFactory.toPolygonArray(polys);
		return polyArray;
	}

	/**
	 * Returns pre-configured optimal max area of individual splits
	 * 
	 * @return pre-configured max area
	 */
	public static double getMaxArea() {
		return MAX_AREA;
	}
}
