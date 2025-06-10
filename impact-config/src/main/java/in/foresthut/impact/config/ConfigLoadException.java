package in.foresthut.impact.config;

import in.foresthut.impact.commons.AlreadyLoggedException;

class ConfigLoadException extends AlreadyLoggedException {

	private static final long serialVersionUID = -975702580036113245L;

	ConfigLoadException(String message, String traceId, Throwable cause) {
		super(message, traceId, cause);
	}

}
