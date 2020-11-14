package jpl.cws.scheduler;

import java.sql.Timestamp;

public class ExternalWorker {
	String id;
	String name;
	String hostname;
	Timestamp lastHeartbeatTime;
	private String activeTopics = null;
	private String currentTopic = null;
	private String currentCommand = null;
	private String currentWorkingDir = null;
	
	public ExternalWorker(String id, String name, String hostname) {
		this.id = id;
		this.name = name;
		this.hostname = hostname;
	}
	
	public String getId() { return id; }
	
	public String getName() { return name; }
	
	public String getHostname() { return hostname; }

	public void setLastHeartbeatTime(Timestamp lastHeartbeatTime) {
		this.lastHeartbeatTime = lastHeartbeatTime;
	}
	
	public Timestamp getLastHeartbeatTime() {
		return lastHeartbeatTime;
	}
	
	public void setActiveTopics(String activeTopics) {
		this.activeTopics = activeTopics;
	}
	
	public String getActiveTopics() {
		return activeTopics;
	}
	
	public void setCurrentTopic(String currentTopic) {
		this.currentTopic = currentTopic;
	}
	
	public String getCurrentTopic() {
		return currentTopic;
	}
	
	public void setCurrentCommand(String currentCommand) {
		this.currentCommand = currentCommand;
	}
	
	public String getCurrentCommand() {
		return currentCommand;
	}
	
	public void setCurrentWorkingDir(String currentWorkingDir) {
		this.currentWorkingDir = currentWorkingDir;
	}
	
	public String getCurrentWorkingDir() {
		return currentWorkingDir;
	}
}

