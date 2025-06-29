package in.foresthut.impact.gemini.exceptions;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;

public class CallLimitExceededException extends AlreadyLoggedException {

	private static final long serialVersionUID = -4275116281815183264L;

	public CallLimitExceededException(String message, String traceId, Throwable cause) {
		super(message, traceId, cause);
	}

}
