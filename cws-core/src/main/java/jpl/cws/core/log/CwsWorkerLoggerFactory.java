package jpl.cws.core.log;

import jpl.cws.core.CwsConfig;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Wrapper around sl4j Logger that prepend CWS worker-specific tags.
 *
 */
public class CwsWorkerLoggerFactory {
	
	@Autowired private CwsConfig cwsConfig;
	
	public CwsWorkerLoggerFactory() {
		System.out.println("CwsWorkerLoggerFactory constructor... cwsConfig=" + cwsConfig);
	}

	public CwsWorkerLogger getLogger(Class clazz) {
		return new CwsWorkerLogger(clazz, cwsConfig.getInstallHostname() , cwsConfig.getWorkerId());
	}
	
}
