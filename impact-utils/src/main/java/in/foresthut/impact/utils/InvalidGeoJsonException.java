package in.foresthut.impact.utils;

class InvalidGeoJsonException extends RuntimeException {

	private static final long serialVersionUID = 9011015601998861322L;
	
	InvalidGeoJsonException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
