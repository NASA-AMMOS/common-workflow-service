package jpl.cws.scheduler;

import java.sql.Timestamp;
import java.util.Map;

public class CwsProcessInstance {

	private String uuid;  // from CWS cws_sched_worker_proc_inst table
	
	private String procDefKey;
	private String procInstId;
	private String superProcInstId;
	private String status;
	
	private String initiationKey;        // from CWS cws_sched_worker_proc_inst table
	
	private Timestamp createdTimestamp;  // from CWS cws_sched_worker_proc_inst table
	private Timestamp updatedTimestamp;  // from CWS cws_sched_worker_proc_inst table
	
	private String claimedByWorker;   // from CWS cws_sched_worker_proc_inst table
	private String startedByWorker;   // from CWS cws_sched_worker_proc_inst table
	
	private Timestamp procStartTime;  // from Camunda ACT_HI_PROCINST_ table
	private Timestamp procEndTime;    // from Camunda ACT_HI_PROCINST_ table

	private Map<String, String> inputVariables;
	
	public CwsProcessInstance(
			String uuid,
			String procDefKey,
			String procInstId,
			String superProcInstId,
			String status,
			String initiationKey,
			Timestamp createdTimestamp,
			Timestamp updatedTimestamp,
			String claimedByWorker,
			String startedByWorker,
			Timestamp procStartTime,
			Timestamp procEndTime,
			Map<String, String> inputVariables) {
		super();
		this.uuid = uuid;
		this.procDefKey = procDefKey;
		this.procInstId = procInstId;
		this.superProcInstId = superProcInstId;
		this.status = status;
		this.initiationKey = initiationKey;
		this.createdTimestamp = createdTimestamp;
		this.updatedTimestamp = updatedTimestamp;
		this.claimedByWorker = claimedByWorker;
		this.startedByWorker = startedByWorker;
		this.procStartTime = procStartTime;
		this.procEndTime = procEndTime;
		this.inputVariables = inputVariables;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getProcDefKey() {
		return procDefKey;
	}
	public void setProcDefKey(String procDefKey) {
		this.procDefKey = procDefKey;
	}
	public String getProcInstId() {
		return procInstId;
	}
	public void setProcInstId(String procInstId) {
		this.procInstId = procInstId;
	}
	public String getSuperProcInstId() {
		return superProcInstId;
	}
	public void setSuperProcInstId(String superProcInstId) {
		this.superProcInstId = superProcInstId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setInputVariables(Map<String, String> input) {
		this.inputVariables = input;
	}

	public Map<String, String> getInputVariables() {
		return inputVariables;
	}
	
	public String getInitiationKey() {
		return initiationKey;
	}
	public void setInitiationKey(String initiationKey) {
		this.initiationKey = initiationKey;
	}
	
	public Timestamp getCreatedTimestamp() {
		return createdTimestamp;
	}
	public void setCreatedTimestamp(Timestamp createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}
	
	public Timestamp getUpdatedTimestamp() {
		return updatedTimestamp;
	}
	public void setUpdatedTimestamp(Timestamp updatedTimestamp) {
		this.updatedTimestamp = updatedTimestamp;
	}
	
	public String getClaimedByWorker() {
		return claimedByWorker;
	}
	public void setClaimedByWorker(String claimedByWorker) {
		this.claimedByWorker = claimedByWorker;
	}
	
	public String getStartedByWorker() {
		return startedByWorker;
	}
	public void setStartedByWorker(String startedByWorker) {
		this.startedByWorker = startedByWorker;
	}
	
	public Timestamp getProcStartTime() {
		return procStartTime;
	}
	public void setProcStartTime(Timestamp procStartTime) {
		this.procStartTime = procStartTime;
	}
	
	public Timestamp getProcEndTime() {
		return procEndTime;
	}
	public void setProcEndTime(Timestamp procEndTime) {
		this.procEndTime = procEndTime;
	}
	
}
