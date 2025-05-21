package in.foresthut.impact.utils.exceptions;

import org.locationtech.jts.io.ParseException;

public class InvalidGeoJsonException extends Exception {

	public InvalidGeoJsonException(String msg, ParseException ex) {
		super(msg, ex);
	}

	private static final long serialVersionUID = 9011015601998861322L;

}
