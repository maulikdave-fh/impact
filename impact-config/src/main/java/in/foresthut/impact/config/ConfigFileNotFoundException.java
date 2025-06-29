package in.foresthut.impact.config;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;

class ConfigFileNotFoundException extends AlreadyLoggedException {

	private static final long serialVersionUID = 8322307244374480798L;

	ConfigFileNotFoundException(String msg, String traceId, Throwable cause) {
		super(msg, traceId, cause);
	}

}
