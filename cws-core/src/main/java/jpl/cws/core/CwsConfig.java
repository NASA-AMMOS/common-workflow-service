package jpl.cws.core;

import org.springframework.beans.factory.annotation.Value;

/**
 * Centralized utility class to access general CWS properties.
 *
 */
public class CwsConfig {

	// These properties are fetched from the properties files
	//
	@Value("${cws.install.hostname}")        private String  installHostname;
	@Value("${cws.install.dir:@null}")       private String  installDir;
	@Value("${cws.tomcat.lib:@null}")        private String  tomcatLib;
	@Value("${cws.tomcat.bin:@null}")        private String  tomcatBin;
	@Value("${cws.tomcat.home:@null}")       private String  tomcatHome;
	@Value("${cws.tomcat.webapps:@null}")    private String  tomcatWebapps;
	@Value("${cws.worker.id:@null}")         private String  workerId;
	@Value("${cws.db.host:@null}")           private String  dbHost;
	@Value("${cws.db.port:@null}")           private Integer dbPort;
	@Value("${cws.db.name:@null}")           private String  dbName;
	@Value("${cws.db.username:@null}")       private String  dbUser;
	@Value("${cws.db.password:@null}")       private String  dbPass;
	@Value("${cws.console.port.ssl:@null}")  private Integer consoleSslPort;
	@Value("${cws.auth.scheme:@null}")       private String  authScheme;
	
	public String getInstallHostname() {
		return installHostname;
	}
	
	public String getInstallDir() {
		return installDir;
	}
	
	public String getTomcatLib() {
		return tomcatLib;
	}

	public String getTomcatBin() {
		return tomcatBin;
	}

	public String getTomcatHome() {
		return tomcatHome;
	}

	public String getTomcatWebapps() { return tomcatWebapps; }
	
	public String getWorkerId() {
		return workerId;
	}
	
	public String getDbHost() {
		return dbHost;
	}
	
	public int getDbPort() {
		return dbPort;
	}
	
	public String getDbName() {
		return dbName;
	}
	
	public String getDbUser() {
		return dbUser;
	}
	
	public String getDbPass() {
		return dbPass;
	}
	
	public int getConsoleSslPort() {
		return consoleSslPort; 
	}
	
	public String getAuthScheme() {
		return authScheme;
	}
}
