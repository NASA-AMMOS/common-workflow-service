package jpl.cws.scheduler;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Worker {
	String id;
	String name;
	Timestamp lastHeartbeatTime;
	private String status;
	
	private String cwsInstallType;
	private String cwsWorkerType;
	
	private int jobExecutorMaxPoolSize;
	
	// If Integer value (limit) is null,
	// that means process is not allowed on this worker
	private Map<String,Integer> procInstanceLimits = new HashMap<String,Integer>();
	
	private Map<String,Boolean> procDefAcceptingNew = new HashMap<String,Boolean>();
	
	public Worker(String id, String name, String cwsInstallType, String cwsWorkerType, String status) {
		this.id = id;
		this.name = name;
		this.cwsInstallType = cwsInstallType;
		this.cwsWorkerType = cwsWorkerType;
		this.status = status;
	}
	
	public String getId() { return id; }
	
	public String getName() { return name; }

	public void setLastHeartbeatTime(Timestamp lastHeartbeatTime) {
		this.lastHeartbeatTime = lastHeartbeatTime;
	}
	
	public Timestamp getLastHeartbeatTime() {
		return lastHeartbeatTime;
	}
	
	
	public Map<String,Integer> getProcInstanceLimits() {
		return procInstanceLimits;
	}
	public void setProcInstanceLimits(Map<String,Integer> procInstanceLimits) {
		this.procInstanceLimits = procInstanceLimits;
	}
	
	
	public Map<String,Boolean> getProcDefAcceptingNew() {
		return procDefAcceptingNew;
	}
	public void setProcDefAcceptingNew(Map<String,Boolean> procDefAcceptingNew) {
		this.procDefAcceptingNew = procDefAcceptingNew;
	}
	
	
	public int getJobExecutorMaxPoolSize() {
		return jobExecutorMaxPoolSize;
	}
	public void setJobExecutorMaxPoolSize(int jobExecutorMaxPoolSize) {
		this.jobExecutorMaxPoolSize = jobExecutorMaxPoolSize;
	}
	
	public String getCwsInstallType() {
		return cwsInstallType;
	}
	public void setCwsInstallType(String cwsInstallType) {
		this.cwsInstallType = cwsInstallType;
	}

	public String getCwsWorkerType() {
		return cwsWorkerType;
	}
	public void setCwsWorkerType(String cwsWorkerType) {
		this.cwsWorkerType = cwsWorkerType;
	}

	public int getEnabledCount() {
		int count = 0;
		for (Object val : procInstanceLimits.values()) {
			if (val != null) { count++; }
		}
		return count;
	}
	
	
	public String getStatus() {
		return status;
	}
}

