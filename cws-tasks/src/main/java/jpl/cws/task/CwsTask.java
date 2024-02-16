package jpl.cws.task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import jpl.cws.core.service.ProcessService;
import jpl.cws.core.service.SpringApplicationContext;

/**
 * Abstract base class for all CWS built-in (and custom user-added) task
 * implementations.
 * 
 */
public abstract class CwsTask implements JavaDelegate {

	public static final String UNEXPECTED_ERROR = "unexpectedError";

	protected final CwsTaskLogger log = new CwsTaskLogger(this.getClass().getName());

	// Actual DB column is 4000 characters, but make this a bit smaller just for
	// safety. If variables go over this limit, then a warning message is added
	// to the end.
	private static final int STRING_MAX_SIZE = 3800;
	private static final String TRUNCATION_MESSAGE = "\n******* OUTPUT TRUNCATED TO FIRST " + STRING_MAX_SIZE + " CHARACTERS!!! *******";
	private Expression throwOnTruncatedVariable;
	private boolean throwOnTruncatedVariableBoolean;
	private static final boolean DEFAULT_THROW_ON_TRUNCATED_VARIABLE = false;

	protected DelegateExecution execution;

	private Expression preCondition;
	// passes (true) by default
	private static final boolean DEFAULT_PRE_CONDITION = true;

	private Expression onPreConditionFail;
	private static final String DEFAULT_ON_PRE_CONDITION_FAIL = PreConditionFailBehavior.SKIP_TASK
			.name();

	private enum PreConditionFailBehavior {
		SKIP_TASK, ABORT_PROCESS
	};

	public CwsTask() {
		log.trace("CwsTask constructor (" + this + ")");
	}

	/**
	 * The heart of a CWS task execution. This method:
	 *   1) initializes parameters
	 *   2) runs the task implementation
	 *   3) throws any qualified exceptions
	 * 
	 */
	public void execute(final DelegateExecution execution) {
		this.execution = execution;
		
		// setup tags on logging
		log.setProcTags(getProcDefKey(execution),
				execution.getProcessInstanceId(), 
				execution.getActivityInstanceId());
		
		try {
			// setup base params
			throwOnTruncatedVariableBoolean = getBooleanParam(
					throwOnTruncatedVariable, "throwOnTruncatedVariable",
					DEFAULT_THROW_ON_TRUNCATED_VARIABLE);
			
			// Evaluate preCondition.
			// If preCondition passes, then execute task,
			// otherwise skip task execution.
			if (evaluateTaskPreCondition()) {
				setOutputVariable("preConditionPassed", true);
				
				// get params
				log.trace("INITIALIZING PARAMETERS FOR TASK: " + this);
				initParams();
				
				// execute the task
				log.trace("EXECUTING TASK: " + this);
				executeTask();
			} else {
				log.warn("SKIPPED TASK EXECUTION BECAUSE PRE-CONDITION DIDN'T PASS: " + this);
				setOutputVariable("preConditionPassed", false);
			}
		} catch (BpmnError e) {
			log.warn("Propagating BpmnError(" + e.getErrorCode() + ")...");
			setOutputVariable("bpmnErrorMessage", e.getErrorCode());
			
			// We saw an error, but we want to check with Camunda because this is by-passing our end-event listener
			notifyWorkerOfFailedProcess();
			
			throw e; // propagate so engine can handle (if boundary catch defined)
		} catch (Throwable t) {
			log.error("Unexpected Throwable while executing " + this, t);
			setOutputVariable("unexpectedErrorMessage", t.getMessage());
			
			notifyWorkerOfFailedProcess();
			
			// wrap and propagate so engine can (if boundary catch defined) handle
			throw new BpmnError(UNEXPECTED_ERROR);
		} finally {
			// cleanup
			this.execution = null;
		}
	}
	

	private void notifyWorkerOfFailedProcess() {
		log.debug("notifying workers of failed process...");
		try {
			// This delay is necessary, because the process must actually
			// complete in Camunda before we can look at Camunda's records
			// to get the true status of the process instance.
			TimeUnit.SECONDS.sleep(2);
			ProcessService cwsProcessService = (ProcessService) SpringApplicationContext.getBean("cwsProcessService");
			cwsProcessService.sendProcEventTopicMessageWithRetries(null, null, null, null, "sync");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	private String getProcDefKey(final DelegateExecution execution) {
		String procDefKey = "UNKNOWN";

		BpmnModelInstance bpmnModelInstance = execution.getBpmnModelInstance();

		if (bpmnModelInstance == null) {
			log.warn("bpmnModelInstance is null.  This should only occur in unit tests!");
		} else {
			procDefKey = bpmnModelInstance
					.getModelElementsByType(
							org.camunda.bpm.model.bpmn.instance.Process.class)
					.iterator().next().getAttributeValue("id");
			// if (procDefKey == null) {
			// // See:
			// https://groups.google.com/forum/#!topic/camunda-bpm-users/anXc5jwV6nA
			// log.warn("procDefKey not determined from model! Trying second approach...");
			// procDefKey =
			// repositoryService.getProcessDefinition(execution.getProcessDefinitionId()).getKey();
			// }
			if (procDefKey == null) {
				log.error("procDefKey unable to be determined!");
			}
		}

		return procDefKey;
	}

	/**
	 * Implementation must be filled out by subclasses.
	 * 
	 */
	protected abstract void initParams() throws Exception;

	/**
	 * Implementation must be filled out by subclasses.
	 * 
	 */
	protected abstract void executeTask() throws Exception;

	/**
	 * Evaluates the task preCondition.
	 * 
	 * @return true if preCondition passes false if preCondition fails
	 * 
	 * @throws Exception  if unexpected exception occurs
	 * @throws BpmnError  if process is to be determined.
	 */
	private boolean evaluateTaskPreCondition() throws Exception {
		
		if (!getBooleanParam(preCondition, "preCondition",
				DEFAULT_PRE_CONDITION)) {
			
			// Check special case for preCondition is "none" and pass as true
			if (preCondition != null && preCondition.getValue(execution).equals("none")) {
				return true;
			}
			
			log.warn("preCondition was not satisfied");
			PreConditionFailBehavior failBehavior = PreConditionFailBehavior
					.valueOf(getStringParam(onPreConditionFail,
							"preConditionFailBehavior",
							DEFAULT_ON_PRE_CONDITION_FAIL));

			if (failBehavior == PreConditionFailBehavior.SKIP_TASK) {
				log.warn("preCondition failed, and behavior settings dictate task execution will be skipped.");
				return false; // skip executing this task
			} else if (failBehavior == PreConditionFailBehavior.ABORT_PROCESS) {
				log.warn("preCondition failed, and behavior settings dictate process will now abort.");
				throw new BpmnError("Aborting process due to failed task pre-condition");
			}
		}
		log.trace("preCondition passed");
		return true;
	}

	protected void setOutputVariable(String name, Object value) {
		name = execution.getCurrentActivityId() + "_" + name;
		if (value instanceof String) {
			setStringVariable(name, value);
		} else {
			log.trace("Setting non-string process variable [execution = "
					+ execution + "]: " + name + " --> " + value);
			execution.setVariable(name, value);
		}
	}

	// WARNING: if using in tasks inside parallel gateways, there is a chance to violate
	// Camunda's unique key (https://forum.camunda.org/t/deadlock-exceptions/286/4)
	//
	protected void setOutputVariableActualName(String name, Object value) {
		if (value instanceof String) {
			setStringVariable(name, value);
		} else {
			log.trace("Setting non-string process variable [execution = " + execution + "]: " + name + " --> " + value);
			execution.setVariable(name, value);
		}
	}

	private void setStringVariable(String name, Object value) {
		String stringValue = (String) value;
		if (stringValue.length() > STRING_MAX_SIZE) {
			log.warn("Variable " + name + " is too long ("
					+ stringValue.length() + " chars) and will be truncated.");

			// set full value as bytes before truncating
			execution.setVariable(name + "_bytes", stringValue.getBytes());
			// NOTE: You can get the String value back from this _bytes variable
			// by using the ${cws.getVar("foo_bytes")} method..

			// set truncated value as string
			execution.setVariable(name, stringValue.substring(0, STRING_MAX_SIZE) + TRUNCATION_MESSAGE);

			// throw an error, if desired
			if (throwOnTruncatedVariableBoolean) {
				throw new BpmnError("ERROR: Variable " + name + " was truncated.");
			}
		} else {
			execution.setVariable(name, stringValue); // just set normally
		}
	}

	public Expression getThrowOnTruncatedVariable() {
		return throwOnTruncatedVariable;
	}

	public void setThrowOnTruncatedVariable(Expression throwOnTruncatedVariable) {
		this.throwOnTruncatedVariable = throwOnTruncatedVariable;
	}

	protected String getStringParam(Expression expression, String paramName)
			throws Exception {
		if (expression == null) {
			// no default, so throw exception
			throw new Exception("Mandatory parameter '" + paramName + "' not specified");
		}
		return getStringValue(expression.getValue(execution), paramName);
	}

	protected String getStringParam(Expression expression, String paramName,
			String defaultValue) throws Exception {
		if (expression == null) {
			// return default
			return defaultValue;
		}
		return getStringValue(expression.getValue(execution), paramName);
	}

	private String getStringValue(Object value, String paramName)
			throws Exception {
		return String.valueOf(value);
	}

	protected Boolean getBooleanParam(Expression expression, String paramName)
			throws Exception {
		if (expression == null) {
			// no default, so throw exception
			throw new Exception("Mandatory parameter '" + paramName + "' not specified");
		}
		return getBooleanValue(expression.getValue(execution), paramName);
	}

	protected Boolean getBooleanParam(Expression expression, String paramName,
			Boolean defaultValue) throws Exception {
		if (expression == null) {
			// return default
			return defaultValue;
		}
		return getBooleanValue(expression.getValue(execution), paramName);
	}

	private Boolean getBooleanValue(Object value, String paramName)
			throws Exception {
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof String) {
			return Boolean.valueOf((String) value);
		} else {
			// unexpected type
			throw new Exception("Parameter '" + paramName + "' is not compatible with Boolean");
		}
	}

	protected Integer getIntegerParam(Expression expression, String paramName)
			throws Exception {
		if (expression == null) {
			// no default, so throw exception
			throw new Exception("Mandatory parameter '" + paramName + "' not specified");
		}
		return getIntegerValue(expression.getValue(execution), paramName);
	}

	protected Integer getIntegerParam(Expression expression, String paramName,
			Integer defaultValue) throws Exception {
		if (expression == null) {
			// return default
			return defaultValue;
		}
		return getIntegerValue(expression.getValue(execution), paramName);
	}

	private Integer getIntegerValue(Object value, String paramName)
			throws Exception {
		if (value instanceof Integer) {
			return (Integer) value;
		} else if (value instanceof String) {
			return Integer.valueOf((String) value);
		} else {
			// unexpected type
			throw new Exception("Parameter '" + paramName + "' is not compatible with Integer");
		}
	}

	protected Long getLongParam(Expression expression, String paramName)
			throws Exception {
		if (expression == null) {
			// no default, so throw exception
			throw new Exception("Mandatory parameter '" + paramName + "' not specified");
		}
		return getLongValue(expression.getValue(execution), paramName);
	}

	protected Long getLongParam(Expression expression, String paramName,
			Long defaultValue) throws Exception {
		if (expression == null) {
			// return default
			return defaultValue;
		}
		return getLongValue(expression.getValue(execution), paramName);
	}

	private Long getLongValue(Object value, String paramName) throws Exception {
		if (value instanceof Long) {
			return (Long) value;
		} else if (value instanceof String) {
			return Long.valueOf((String) value);
		} else if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		else {
			// unexpected type
			throw new Exception("Parameter '" + paramName + "' is not compatible with Long");
		}
	}

	protected Float getFloatParam(Expression expression, String paramName)
			throws Exception {
		if (expression == null) {
			// no default, so throw exception
			throw new Exception("Mandatory parameter '" + paramName + "' not specified");
		}
		return getFloatValue(expression.getValue(execution), paramName);
	}

	protected Float getFloatParam(Expression expression, String paramName,
			Float defaultValue) throws Exception {
		if (expression == null) {
			// return default
			return defaultValue;
		}
		return getFloatValue(expression.getValue(execution), paramName);
	}

	private Float getFloatValue(Object value, String paramName)
			throws Exception {
		if (value instanceof Float) {
			return (Float) value;
		} else if (value instanceof String) {
			return Float.valueOf((String) value);
		} else {
			// unexpected type
			throw new Exception("Parameter '" + paramName + "' is not compatible with Float");
		}
	}

	protected Double getDoubleParam(Expression expression, String paramName)
			throws Exception {
		if (expression == null) {
			// no default, so throw exception
			throw new Exception("Mandatory parameter '" + paramName + "' not specified");
		}
		return getDoubleValue(expression.getValue(execution), paramName);
	}

	protected Double getDoubleParam(Expression expression, String paramName,
			Double defaultValue) throws Exception {
		if (expression == null) {
			// return default
			return defaultValue;
		}
		return getDoubleValue(expression.getValue(execution), paramName);
	}

	private Double getDoubleValue(Object value, String paramName)
			throws Exception {
		if (value instanceof Double) {
			return (Double) value;
		} else if (value instanceof String) {
			return Double.valueOf((String) value);
		} else {
			// unexpected type
			throw new Exception("Parameter '" + paramName + "' is not compatible with Double");
		}
	}

	/**
	 * For now only supports Map<String,String> but may want to support
	 * generics/others in future...
	 * 
	 */
	protected Map<String, String> getMapParam(Expression expression,
			String paramName) throws Exception {
		if (expression == null) {
			// no default, so throw exception
			throw new Exception("Mandatory parameter '" + paramName + "' not specified");
		}
		return getMapValue(expression.getValue(execution), paramName);
	}

	protected Map<String, String> getMapParam(Expression expression,
			String paramName, Map<String, String> defaultValue)
			throws Exception {
		if (expression == null) {
			// return default
			return defaultValue;
		}
		return getMapValue(expression.getValue(execution), paramName);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getMapValue(Object value, String paramName)
			throws Exception {
		if (value instanceof Map) {
			return (Map<String, String>) value;
		} else if (value instanceof String) {
			Map<String, String> paramValue = new HashMap<String, String>();
			String[] keyValPairs = ((String) value).split(",");
			for (String keyValPair : keyValPairs) {
				String[] keyAndVal = keyValPair.split("=");
				if (paramValue.put(keyAndVal[0], keyAndVal[1]) != null) {
					throw new Exception(
							"Duplicate key ('"
									+ keyAndVal[0]
									+ "') specfied in map!  This is not allowed, as it can lead to ambiguities.");
				}
			}
			return paramValue;
		} else {
			// unexpected type
			throw new Exception("Parameter '" + paramName + "' is not compatible with Map");
		}
	}

}
