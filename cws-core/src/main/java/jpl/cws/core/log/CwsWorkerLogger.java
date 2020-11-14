package jpl.cws.core.log;


/**
 * Wrapper around sl4j Logger that prepend CWS worker-specific tags.
 *
 */
public class CwsWorkerLogger extends CwsLogger {
	
	public CwsWorkerLogger(Class clazz, String cwsInstallHostname, String workerId) {
		super(clazz);
		setTag("[" + cwsInstallHostname + "][" + workerId + "] ");
	}

}
