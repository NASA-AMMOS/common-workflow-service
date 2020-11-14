package jpl.cws.core.web;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerInfo {

	private static final Logger log = LoggerFactory.getLogger(WorkerInfo.class);
	
	String name;
	Long diskFreeBytes;

	List<LogInfo> logs = new ArrayList<LogInfo>();
	
	public WorkerInfo(String name, Object diskFree) {
		
		this.name = name;

		try {
			
			if (diskFree != null) {

				diskFreeBytes = Long.parseLong(diskFree.toString());
			}
		}
		catch (Exception e) {

			log.error("Failed to construct WorkerInfo.", e);
		}
	}
	
	public String getName() { return name; }
	
	public Long getDiskFreeBytes() { return diskFreeBytes; }
	
	public List<LogInfo> getLogs() { return logs; }
}
