package in.foresthut.impact.ecoregion.service;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.sun.net.httpserver.HttpExchange;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.ecoregion.exceptions.InvalidRegionNameException;
import in.foresthut.impact.ecoregion.repo.EarthDatabase;
import in.foresthut.impact.utils.geometry.Polygon;
import in.foresthut.impact.utils.net.BaseHttpHandler;

public class CombineEcoregionsMapHandler extends BaseHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(CombineEcoregionsMapHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (exchange.getRequestMethod().equals("GET")) {
			String[] path = exchange.getRequestURI().getPath().split("/");
			if (path.length != 4) {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid Request '{}'", traceId, exchange.getRequestURI().getPath());
				sendResponse("Invalid Request. Check for typos.", exchange, 404, traceId);
				return;
			}
			String regionName = URLDecoder.decode(path[2], "UTF-8");
			logger.info("Combine ecoregion maps request received for region {}", regionName);

			String action = path[3];
			if (!action.equals("combine")) {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid Action in Request '/{}'. Check for typo.", traceId, action);
				sendResponse("Invalid Request. Check for typos.", exchange, 404, traceId);
				return;
			}

			try {
				List<String> ecoregionMaps = ecoregionMaps(regionName);
				if (ecoregionMaps.isEmpty()) {
					final String traceId = UUID.randomUUID().toString();
					sendResponse("Invalid region name.", exchange, 404, traceId);
					return;
				}
				String combinedMap = Polygon.combine(ecoregionMaps, true);
				sendResponse(combinedMap, exchange, 200, null);
			} catch (InvalidRegionNameException e) {
				final String traceId = e.traceId();
				sendResponse("Invalid ecoregion id.", exchange, 404, traceId);
			} catch (AlreadyLoggedException ex) {
				sendResponse("Oops! This shouldn't have been happened.", exchange, 500, ex.traceId());
			} catch (Exception ex) {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Unexpected error", traceId, ex);
				sendResponse("Oops! This shouldn't have been happened.", exchange, 500, traceId);
			}
		} else {
			final String traceId = UUID.randomUUID().toString();
			logger.error("[Error-trace-id] {} Invalid request method {}", traceId, exchange.getRequestMethod());
			sendResponse("Invalid Request Method.", exchange, 405, traceId);
		}

	}

	private List<String> ecoregionMaps(String regionName) {
		var earthDB = EarthDatabase.getInstance();
		var ecoregionsCollection = earthDB.getCollection("ecoregion");

		Bson filter = Filters.eq("region", regionName);
		// 2. Define the projection (include 'name' and 'age' fields, exclude '_id')
		Bson projection = Projections.fields(Projections.include("regionMap"), Projections.excludeId());
		var ecoregionMaps = ecoregionsCollection.find(filter).projection(projection);
		List<String> wtkPolygons = new ArrayList<>();
		for (var ecoregionMap : ecoregionMaps) {
			wtkPolygons.add(ecoregionMap.getString("regionMap"));
		}

		return wtkPolygons;
	}
}
