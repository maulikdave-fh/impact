package in.foresthut.impact.ecoregion.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;
import in.foresthut.impact.ecoregion.exceptions.InvalidEcoregionIdException;
import in.foresthut.impact.ecoregion.repo.Ecoregion;
import in.foresthut.impact.utils.net.BaseHttpHandler;

public class EcoregionSplitHandler extends BaseHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(EcoregionSplitHandler.class);

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
			String id = path[2];

			String action = path[3];
			if (!action.equals("split")) {
				final String traceId = UUID.randomUUID().toString();
				logger.error("[Error-trace-id] {} Invalid Action in Request '/{}'. Check for typo.", traceId, action);
				sendResponse("Invalid Request. Check for typos.", exchange, 404, traceId);
				return;
			}

			List<String> splits = new ArrayList<>();
			try {
				splits = Ecoregion.split(id);
				sendResponse(splits, exchange, 200, null);
			} catch (InvalidEcoregionIdException e) {
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

}
