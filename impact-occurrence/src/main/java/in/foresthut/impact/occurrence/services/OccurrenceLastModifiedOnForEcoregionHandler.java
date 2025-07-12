package in.foresthut.impact.occurrence.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import in.foresthut.impact.occurrence.repo.OccurrenceRepository;
import in.foresthut.impact.utils.net.BaseHttpHandler;

public class OccurrenceLastModifiedOnForEcoregionHandler extends BaseHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(OccurrenceLastModifiedOnForEcoregionHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (exchange.getRequestMethod().equals("GET")) {
			// https://<host>:<port>/modifiedOn/<regionName>/<ecoregionid>
			String[] path = exchange.getRequestURI().getPath().split("/");
			if (path.length == 4) {
				String regionName = URLDecoder.decode(path[2], "UTF-8");
				String ecoregionId = path[3];
				var latestModifiedOn = OccurrenceRepository.getInstance().latestModifiedOn(ecoregionId, regionName);
		
				if (latestModifiedOn != null){
					sendResponse(latestModifiedOn, exchange, 200, null);
				} else {
					sendResponse(null, exchange, 200, null);
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
