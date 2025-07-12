package in.foresthut.impact.ecoregion.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.ecoregion.repo.Ecoregion;
import in.foresthut.impact.ecoregion.repo.Ecoregions;
import in.foresthut.impact.utils.net.BaseHttpHandler;

public class EcoregionsHandler extends BaseHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(EcoregionsHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (exchange.getRequestMethod().equals("GET")) {
			String[] path = exchange.getRequestURI().getPath().split("/");
			if (path.length > 2) {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid path {}", traceId, exchange.getRequestURI().getPath());
				sendResponse("Invalid Request", exchange, 400, traceId);
				return;
			}
			try {
				List<Ecoregion> ecoregions = Ecoregions.get();
				sendResponse(ecoregions, exchange, 200, null);
			} catch (AlreadyLoggedException ex) {
				final String traceId = ex.traceId();
				sendResponse("Oops! This shouldn't have been happened.", exchange, 500, traceId);
			} catch (Exception ex) {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Unexpected error", traceId, ex);
				sendResponse("Oops! This shouldn't have been happened.", exchange, 500, traceId);
			}
		} else {
			final String traceId = UUID.randomUUID().toString();
			logger.error("[Error-trace-id] {} Invalid request method {}", traceId, exchange.getRequestMethod());
			sendResponse("Invalid Request Method", exchange, 405, traceId);
		}
	}
}
