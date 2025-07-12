package in.foresthut.impact.utils.net;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class BaseHttpHandler implements HttpHandler {

	@Override
	public abstract void handle(HttpExchange exchange) throws IOException;

	protected static void sendResponse(Object responseObj, HttpExchange exchange, int httpStatusCode, String uuid)
			throws IOException {
		String jsonString = new ObjectMapper().writeValueAsString(responseObj);
		byte[] response = jsonString.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		if (uuid != null)
			exchange.getResponseHeaders().set("Error-Trace-Id", uuid);
		exchange.sendResponseHeaders(httpStatusCode, response.length);
		OutputStream outStream = exchange.getResponseBody();
		outStream.write(response);
		outStream.close();
	}
}
