package in.foresthut.impact.utils;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;

class InvalidGeoJsonException extends AlreadyLoggedException {

	private static final long serialVersionUID = 9011015601998861322L;
	
	InvalidGeoJsonException(String msg, String traceId, Throwable cause) {
		super(msg, traceId, cause);
	}
}
