package jpl.cws.task;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jpl.cws.core.log.CwsLogger;

/**
 * Wrapper around sl4j Logger that prepend CWS task-specific tags.
 * 
 *
 */
public class CwsTaskLogger extends CwsLogger {

	// static fields -- only loaded (not on every task construction)
	private static String hostname;
	private static String workerId;
	
	private String procInstanceId;
	private String procDefKey;
	private String activityInstanceId;
	
	public CwsTaskLogger(String clazzName) {
		super(clazzName);
		
		// Get hostname and/or workerId from properties file
		//
		if (hostname == null || workerId == null) {
			initHostnameAndWorkerId();
		}
		
		setTag("[" + hostname + "][" + workerId + "][" + procDefKey + "][" + procInstanceId + "][" + activityInstanceId + "] ");
	}
	
	
	private void initHostnameAndWorkerId() {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("cws-engine.properties");
		if (in != null) {
			Properties props = new Properties();
			try {
				props.load(in);
				if (hostname == null) {
					hostname = props.getProperty("cws.install.hostname");
				}
				if (workerId == null) {
					workerId = props.getProperty("cws.worker.id");
				}
			} catch (IOException e) {
				log.error("Unable to load properties from " + in);
				hostname = "UNKNOWN";
			}
		}
		else {
			log.error("InputStream for cws-engine.properties was null!");
			hostname = "UNKNOWN";
		}
		try {
			in.close();
		} catch (IOException e) {
			log.error("Unexpected exception when closing InputStream for cws-engine.properties");
			in = null;
		}
	}
	
	
	public void setProcTags(String procDefKey, String procInstanceId, String activityInstanceId) {
		this.procDefKey = procDefKey;
		this.procInstanceId = procInstanceId;
		this.activityInstanceId = activityInstanceId;
		setTag("[" + hostname + "][" + workerId + "][" + procDefKey + "][" + procInstanceId + "][" + activityInstanceId + "] ");
	}
	
	public void setProcInstanceId(String procInstanceId) {
		this.procInstanceId = procInstanceId;
		setTag("[" + hostname + "][" + workerId + "][" + procDefKey + "][" + procInstanceId + "][" + activityInstanceId + "] ");
	}
	
	public void setProcDefKey(String procDefKey) {
		this.procDefKey = procDefKey;
		setTag("[" + hostname + "][" + workerId + "][" + procDefKey + "][" + procInstanceId + "][" + activityInstanceId + "] ");
	}
	
	public void setActivityInstanceId(String activityInstanceId) {
		this.activityInstanceId = activityInstanceId;
		setTag("[" + hostname + "][" + workerId + "][" + procDefKey + "][" + procInstanceId + "][" + activityInstanceId + "] ");
	}
}
