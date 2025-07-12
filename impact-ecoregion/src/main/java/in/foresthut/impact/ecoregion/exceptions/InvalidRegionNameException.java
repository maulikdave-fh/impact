package in.foresthut.impact.ecoregion.exceptions;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;

public class InvalidRegionNameException extends AlreadyLoggedException {
	private static final long serialVersionUID = -3145402068907465529L;

	public InvalidRegionNameException(String msg, String traceId, Throwable cause) {
		super(msg, traceId, cause);
	}
}
