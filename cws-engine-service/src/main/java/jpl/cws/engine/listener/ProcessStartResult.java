package jpl.cws.engine.listener;

import java.util.Map;

/**
 * This class represents the result of a process start attempt.
 * If a process instance was started successfully, the status
 * should be SUCCESS, and processInstanceId should be set.
 * 
 * @author ghollins
 *
 */
public class ProcessStartResult {
	
	public enum Status {
		SUCCESS,
		DID_NOT_ATTEMPT,
		NOTHING_CLAIMED,
		FATAL
	};
	private Status status;
	private Map<String,Object> procReqData;
	private String message;

	public ProcessStartResult(Status status) {
		this.status = status;
	}

	
	public ProcessStartResult(Status status, Map<String,Object> procReqData) {
		this.status      = status;
		this.procReqData = procReqData;
	}
	
	public ProcessStartResult(Status status, Map<String,Object> procReqData, String message) {
		this.status      = status;
		this.procReqData = procReqData;
		this.message     = message;
	}

	public Status getStatus() { return status; }
	public void setStatus(Status status) { this.status = status; }

	public Map<String,Object> getProcReqData() { return procReqData; }
//	public String getProcessInstanceId() { return processInstanceId; }
//	public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
//	
	public String getMessage() { return message; }
	
	public String toString() {
		return "procReqData = " + procReqData + ", status = " + status.name() + ", message = " + message;
	}
	
}
