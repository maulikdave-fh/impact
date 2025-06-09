package in.foresthut.impact.config;

class ConfigFileNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 8322307244374480798L;

	ConfigFileNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	ConfigFileNotFoundException(String msg) {
		super(msg);
	}

}
