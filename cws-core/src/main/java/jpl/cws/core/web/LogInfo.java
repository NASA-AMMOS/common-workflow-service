package jpl.cws.core.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogInfo {

	private static final Logger log = LoggerFactory.getLogger(WorkerInfo.class);
	
	String name;
	Long size;
	
	public LogInfo(String name, Object dbSize) {
		
		this.name = name;
		
		try {
			
			if (dbSize != null) {

				size = Long.parseLong(dbSize.toString());
			}
		}
		catch (Exception e) {

			log.error("Failed to construct LogInfo.", e);
		}
	}
	
	public String getName() { return name; }

	public Long getSize() { return size; }
}
