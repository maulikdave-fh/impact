package in.foresthut.impact.commons;

public class AlreadyLoggedException extends RuntimeException {

	private static final long serialVersionUID = 2686665991056270371L;
	private String traceId;

	public AlreadyLoggedException(String message, String traceId, Throwable cause) {
		super(message, cause);
		this.traceId = traceId;
	}

	public String traceId() {
		return traceId;
	}

}
