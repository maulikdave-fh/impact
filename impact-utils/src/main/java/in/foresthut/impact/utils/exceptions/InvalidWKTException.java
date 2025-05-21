package in.foresthut.impact.utils.exceptions;

import org.locationtech.jts.io.ParseException;

public class InvalidWKTException extends Exception {

	private static final long serialVersionUID = -3801470378633500291L;

	public InvalidWKTException(String msg, ParseException ex) {
		super(msg, ex);
	}

}
