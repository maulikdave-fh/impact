package in.foresthut.impact.occurrence.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import in.foresthut.impact.occurrence.repo.OccurrenceRepository;
import in.foresthut.impact.utils.net.BaseHttpHandler;

public class OccurrenceExistsHandler extends BaseHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(OccurrenceExistsHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (exchange.getRequestMethod().equals("GET")) {
			// https://<host>:<port>/occurrences/<ecoregionid>/<gbifkey>/exists
			String[] path = exchange.getRequestURI().getPath().split("/");
			if (path.length == 5 && path[4].equals("exists")) {
				String regionName = URLDecoder.decode(path[2], "UTF-8");
				String occurrenceKey = path[3];
				// TODO: Input validation for ecoregionId and occurrenceKey
				if (OccurrenceRepository.getInstance().exists(regionName, occurrenceKey)) {
					sendResponse(true, exchange, 200, null);
				} else {
					sendResponse(false, exchange, 200, null);
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
