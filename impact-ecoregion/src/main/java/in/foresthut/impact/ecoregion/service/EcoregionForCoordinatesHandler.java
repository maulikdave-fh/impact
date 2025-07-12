package in.foresthut.impact.ecoregion.service;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import in.foresthut.impact.ecoregion.repo.Ecoregions;
import in.foresthut.impact.utils.net.BaseHttpHandler;

public class EcoregionForCoordinatesHandler extends BaseHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(EcoregionForCoordinatesHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (exchange.getRequestMethod().equals("GET")) {
			// https://<host>:<port>/ecoregionid/for/<latitude>/<longitude>
			String[] path = exchange.getRequestURI().getPath().split("/");
			if (path.length == 5) {
				double latitude = 0.0, longitude = 0.0;
				try {
					latitude = Double.valueOf(path[3]);
					longitude = Double.valueOf(path[4]);

					var ecoregion = Ecoregions.ecoregionContaining(latitude, longitude);
					if (ecoregion != null) {
						logger.info("The coordinates ({}, {}) fall in ecoregion {}", latitude, longitude,
								ecoregion.id());
						sendResponse(ecoregion, exchange, 200, null);
						return;
					}
					logger.info("No matching ecoregion found for the coordinates ({}, {})", latitude, longitude);
					sendResponse(null, exchange, 400, null);

				} catch (Exception ex) {
					final String traceId = UUID.randomUUID().toString();
					logger.error("[Error-trace-id] {} Invalid Request - check coordinates '{}'", traceId,
							exchange.getRequestURI().getPath());
					sendResponse("Invalid Request.", exchange, 400, traceId);
				}
			} else {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid Request '{}'", traceId, exchange.getRequestURI().getPath());
				sendResponse("Invalid Request.", exchange, 400, traceId);
			}
		} else {
			final String traceId = UUID.randomUUID().toString();
			logger.error("[Error-trace-id] {} Invalid Request '{}'", traceId, exchange.getRequestURI().getPath());
			sendResponse("Invalid Request.", exchange, 400, traceId);
		}
	}
}
