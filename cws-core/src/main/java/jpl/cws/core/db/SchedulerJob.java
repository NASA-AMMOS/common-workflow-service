package jpl.cws.core.db;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author ghollins
 *
 */
public class SchedulerJob {
	
	private String uuid;
	private Timestamp createdTime;
	private Timestamp updatedTime;
	private String workerId;
	private String procInstId;
	private String procDefKey;
	private Integer procPriority;
	private Map<String,String> procVariables;
	private String procBusinessKey;
	private String initiationKey;
	private String status;
	private String errorMessage;
	
	public SchedulerJob(
			String uuid,
			Timestamp createdTime,
			Timestamp updatedTime,
			String workerId,
			String procInstId,
			String procDefKey,
			Integer procPriority,
			Map<String,String> procVariables,
			String procBusinessKey,
			String initiationKey,
			String status,
			String errorMessage) {
		this.uuid = uuid;
		this.createdTime = createdTime;
		this.updatedTime = updatedTime;
		this.workerId = workerId;
		this.procInstId = procInstId;
		this.procDefKey = procDefKey;
		this.procPriority = procPriority;
		
		// shallow copy, so we don't mess with source
		this.procVariables = new HashMap<String,String>(procVariables);
		
		this.procBusinessKey = procBusinessKey;
		this.initiationKey = initiationKey;
		this.status = status;
		this.errorMessage = errorMessage;
		
		// put some key information in variables, so they 
		// will be available to running process instances.
		this.procVariables.put("uuid",            this.uuid);
		this.procVariables.put("procDefKey",      this.procDefKey);
		this.procVariables.put("procBusinessKey", this.procBusinessKey);
		this.procVariables.put("procPriority",    this.procPriority+"");
		this.procVariables.put("workerId",        this.workerId);
		this.procVariables.put("initiationKey",   this.initiationKey);
	}
	
	public String getUuid() { return uuid; }
	public void setUuid(String uuid) { this.uuid = uuid; }
	
	public Timestamp getCreatedTime() { return createdTime; }
	public void setCreatedTime(Timestamp createdTime) { this.createdTime = createdTime; }

	public Timestamp getUpdatedTime() { return updatedTime; }
	public void setUpdatedTime(Timestamp updatedTime) { this.updatedTime = updatedTime; }

	public String getWorkerId() { return workerId; }
	public void setWorkerId(String workerId) { this.workerId = workerId; }

	public String getProcInstId() { return procInstId; }
	public void setProcInstId(String procInstId) { this.procInstId = procInstId; }
	
	public String getProcDefKey() { return procDefKey; }
	public void setProcDefKey(String procDefKey) { this.procDefKey = procDefKey; }
	
	public Map<String,String> getProcVariables() { return procVariables; }
	public void setProcVariables(Map<String,String> procVariables) { this.procVariables = procVariables; }
	
	public String getProcBusinessKey() { return procBusinessKey; }
	public void setProcBusinessKey(String procBusinessKey) { this.procBusinessKey = procBusinessKey; }

	public String getInitiationKey() { return initiationKey; }
	public void setInitiationKey(String initiationKey) { this.initiationKey = initiationKey; }
	
	public Integer getProcPriority() { return procPriority; }
	public void setProcPriority(Integer procPriority) { this.procPriority = procPriority; }

	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	public String getErrorMessage() { return errorMessage; }
	public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
	
}
