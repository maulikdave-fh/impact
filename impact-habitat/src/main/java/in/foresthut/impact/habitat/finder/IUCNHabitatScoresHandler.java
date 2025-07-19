package in.foresthut.impact.habitat.finder;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import in.foresthut.impact.habitat.finder.service.IUCNHabitatFinder;
import in.foresthut.impact.utils.net.BaseHttpHandler;

/**
 * Gives IUCN habitat scores for a given text
 */
public class IUCNHabitatScoresHandler extends BaseHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(IUCNHabitatScoresHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (exchange.getRequestMethod().equals("GET")) {
			// https://<host>:<port>/iucn-habitats?text=<text-content>
			String[] path = exchange.getRequestURI().getPath().split("/");
			if (path.length == 2) {
				String[] queryString = exchange.getRequestURI().getQuery().split("=");
				if (queryString[0].equals("text")) {
					var scores= IUCNHabitatFinder.getInstance().calculateTfIdfScore(URLDecoder.decode(queryString[1], "UTF-8"));
					sendResponse(scores, exchange, 200, null);
				} else {
					sendInvalidPathResponse(exchange);
				}
			} else {
				sendInvalidPathResponse(exchange);
			}
		} else {
			sendInvalidPathResponse(exchange);
		}
	}

	private void sendInvalidPathResponse(HttpExchange exchange) throws IOException {
		final String traceId = UUID.randomUUID().toString();
		logger.error("[Error-trace-id] {} Invalid Request '{}'", traceId, exchange.getRequestURI().getPath());
		sendResponse("Invalid Request.", exchange, 400, traceId);
	}

}
