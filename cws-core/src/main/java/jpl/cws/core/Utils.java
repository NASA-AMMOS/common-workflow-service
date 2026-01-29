package jpl.cws.core;

import jpl.cws.core.CwsConfig;
import jpl.cws.core.service.SpringApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger log = LoggerFactory.getLogger(Utils.class);

	private CwsConfig cwsConfig;

	public Utils() {
		if (SpringApplicationContext.isContextAvailable()) {
			cwsConfig = (CwsConfig)SpringApplicationContext.getBean("cwsConfig");
			log.debug("cwsConfig = " + cwsConfig);
		} else {
			log.warn("Spring ApplicationContext not available during Utils construction. cwsConfig will be retrieved later if needed.");
		}
	}

	public String getCwsVar(String name) throws Exception {
		// Get cwsConfig if not already available
		if (cwsConfig == null) {
			if (SpringApplicationContext.isContextAvailable()) {
				cwsConfig = (CwsConfig)SpringApplicationContext.getBean("cwsConfig");
			} else {
				throw new IllegalStateException("Cannot get CWS variable '" + name + "': Spring ApplicationContext is not available and CwsConfig is null.");
			}
		}

		if (name.equals("hostname")) { return cwsConfig.getInstallHostname(); }
		else if (name.equals("installDir")) { return cwsConfig.getInstallDir(); }
		else if (name.equals("tomcatLib")) { return cwsConfig.getTomcatLib();	}
		else if (name.equals("tomcatBin")) { return cwsConfig.getTomcatBin();	}
		else if (name.equals("tomcatHome")) { return cwsConfig.getTomcatHome(); }
		else if (name.equals("tomcatWebapps")) { return cwsConfig.getTomcatWebapps();	}
		else if (name.equals("workerId")) {return cwsConfig.getWorkerId(); }
		else if (name.equals("dbHost")) { return cwsConfig.getDbHost(); }
		else if (name.equals("dbPort")) { return String.valueOf(cwsConfig.getDbPort()); }
		else if (name.equals("dbName")) { return cwsConfig.getDbName(); }
		else if (name.equals("webPort")) { return String.valueOf(cwsConfig.getConsoleSslPort()); }
		else if (name.equals("authScheme")) { return cwsConfig.getAuthScheme(); }
		else {
			throw new Exception("Variable " + name + " not found.");
		}
	}
}
