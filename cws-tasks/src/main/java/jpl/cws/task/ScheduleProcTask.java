package jpl.cws.task;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.delegate.Expression;

import jpl.cws.core.service.ProcessService;
import jpl.cws.core.service.SpringApplicationContext;



/**
 * Built-in task that schedules a process instance.
 * 
 */
public class ScheduleProcTask extends CwsTask {
	// TODO: centralize this variable, since it's in two places right now!
	public static final int DEFAULT_PROCESS_PRIORITY = 10;
	
	protected ProcessService processService;
	
	private Expression procDefKeyExpr;
	private String procDefKey;
	
	private Expression procVariablesExpr;
	private Map<String,String> procVariables;
	
	private Expression procBusinessKeyExpr;
	private String procBusinessKey;
	
	private Expression initiationKeyExpr;
	private String initiationKey;
	
	private Expression priorityExpr;
	private int priority;
	
	public ScheduleProcTask() {
		log.debug("ScheduleProcTask constructor...");
		processService = (ProcessService) SpringApplicationContext.getBean("cwsProcessService");
		log.debug("ScheduleProcTask() processService = " + processService);
	}

	@Override
	public void initParams() throws Exception {
		procDefKey      = getStringParam(procDefKeyExpr, "procDefKey");
		procVariables   = getMapParam(procVariablesExpr, "processVariables", new HashMap<String,String>());
		procBusinessKey = getStringParam(procBusinessKeyExpr, "procBusinessKey", null);
		initiationKey   = getStringParam(initiationKeyExpr, "initiationKey", null);
		priority        = getIntegerParam(priorityExpr, "priority", DEFAULT_PROCESS_PRIORITY);
	}

	@Override
	public void executeTask() throws Exception {
		this.setOutputVariable("procDefKey", procDefKey);
		processService.sendProcScheduleMessageWithRetries(
				procDefKey, procVariables, 
				procBusinessKey, initiationKey, priority);
	}

	public Expression getProcDefKey() {
		return procDefKeyExpr;
	}

	public void setProcDefKey(Expression procDefKeyExpr) {
		this.procDefKeyExpr = procDefKeyExpr;
	}

	public Expression getProcDefKeyExpr() {
		return procDefKeyExpr;
	}

	public void setProcDefKeyExpr(Expression procDefKeyExpr) {
		this.procDefKeyExpr = procDefKeyExpr;
	}

	public Expression getProcVariablesExpr() {
		return procVariablesExpr;
	}

	public void setProcVariablesExpr(Expression procVariablesExpr) {
		this.procVariablesExpr = procVariablesExpr;
	}

	public Expression getProcBusinessKeyExpr() {
		return procBusinessKeyExpr;
	}

	public void setProcBusinessKeyExpr(Expression procBusinessKeyExpr) {
		this.procBusinessKeyExpr = procBusinessKeyExpr;
	}

	public Expression getInitiationKeyExpr() {
		return initiationKeyExpr;
	}

	public void setInitiationKeyExpr(Expression initiationKeyExpr) {
		this.initiationKeyExpr = initiationKeyExpr;
	}

	public Expression getPriorityExpr() {
		return priorityExpr;
	}

	public void setPriorityExpr(Expression priorityExpr) {
		this.priorityExpr = priorityExpr;
	}
	
}
