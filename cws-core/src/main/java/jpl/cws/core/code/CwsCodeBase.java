package jpl.cws.core.code;

import java.util.UUID;

import jpl.cws.core.CwsConfig;

public class CwsCodeBase {
	protected CwsConfig cwsConfig;

	//------- Config Getters --------
	// note: some getters have two functions to preserve backwards compatibility
	//       e.g. getHostname and getCwsHostname allow for both
	//       cws.hostname (new) and cws.cwsHostname (old)

	public String getHostname() {
		return cwsConfig.getInstallHostname();
	}

	public String getCwsHostname() {
		return cwsConfig.getInstallHostname();
	}

	public int getWebPort() {
		return cwsConfig.getConsoleSslPort();
	}
	
	public String getInstallDir() {
		return cwsConfig.getInstallDir();
	}

	public String getCwsInstallDir() {
		return cwsConfig.getInstallDir();
	}
	
	public String getWorkerId() {
		return cwsConfig.getWorkerId();
	}

	public String getTomcatLib() {
		return cwsConfig.getTomcatLib();
	}

	public String getTomcatHome() {
		return cwsConfig.getTomcatHome();
	}

	public String getTomcatBin() {
		return cwsConfig.getTomcatBin();
	}

	public String getTomcatWebapps() {
		return cwsConfig.getTomcatWebapps();
	}

	public String getDbHost() {
		return cwsConfig.getDbHost();
	}

	public int getDbPort() {
		return cwsConfig.getDbPort();
	}

	public String getDbName() {
		return cwsConfig.getDbName();
	}

	public String getAuthScheme() {
		return cwsConfig.getAuthScheme();
	}

	//------- Utils --------

	public String getEnv(String envVar) {
		return System.getenv(envVar);
	}

	public String getCwsEnv(String envVar) {
		return System.getenv(envVar);
	}

	public String getRandUuid() {
		return UUID.randomUUID().toString();
	}

	/**
	 * This is necessary for reconstructing the cws bean - when configuration is
	 * edited, this method is called to reload the config
	 * @param cwsConfig CWS configuration object
	 */
	public void setCwsConfig(CwsConfig cwsConfig) {
		this.cwsConfig = cwsConfig;
	}
}