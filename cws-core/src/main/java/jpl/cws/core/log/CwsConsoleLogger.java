package jpl.cws.core.log;


/**
 * Wrapper around sl4j Logger that prepend CWS worker-specific tags.
 *
 */
public class CwsConsoleLogger extends CwsLogger {
	
	public CwsConsoleLogger(Class clazz, String cwsInstallHostname, String workerId) {
		super(clazz);
		setTag("[" + cwsInstallHostname + "][" + workerId + "] ");
	}

}
