package in.foresthut.impact.ecoregion.exceptions;

import in.foresthut.impact.commons.exceptions.AlreadyLoggedException;

public class InvalidEcoregionIdException extends AlreadyLoggedException {

	private static final long serialVersionUID = 4623993105548360299L;

	public InvalidEcoregionIdException(String msg, String traceId, Throwable cause) {
		super(msg, traceId, cause);
	}

}
