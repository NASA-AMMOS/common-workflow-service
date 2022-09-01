package jpl.cws.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpl.cws.core.CmdLineInputFields;
import jpl.cws.core.CmdLineOutputFields;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.executor.BatchExecutorException;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;

import com.google.gson.Gson;

import edu.rice.cs.util.ArgumentTokenizer;
import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Collections2;
import jpl.cws.task.CwsTaskLogger;
import org.camunda.spin.json.SpinJsonNode;
import org.camunda.spin.plugin.variable.SpinValues;
import org.camunda.spin.plugin.variable.value.JsonValue;

public class CwsExternalTaskThread extends Thread  {

	private RuntimeService runtimeService;
	private ExternalTaskService externalTaskService;
	private WorkerService workerService;
	
	protected final CwsTaskLogger log = new CwsTaskLogger(this.getClass().getName());
	
	private static AtomicInteger globalOutOrdering = new AtomicInteger(1000000);
	private static final String EXECUTION_NOT_SUCCESS = "executionNotSuccess";
	
	private static final String CWS_VARS_FROM_STDOUT_REGEX = "\\\"?cwsVariables\\\"?\\s*:\\s*(\\{(.+?)\\})";

	public static final String UNEXPECTED_ERROR = "unexpectedError";
	public static final String TIMEOUT_ERROR = "timeoutError";
	public static final String TRUNCATED_ERROR = "truncatedError";
	public static final String WORKING_DIR_NOT_FOUND_ERROR = "workingDirNotFoundError";
	
	// Actual DB column is 4000 characters, but make this a bit smaller just for
		// safety. If variables go over this limit, then a warning message is added
		// to the end.
	private static final int STRING_MAX_SIZE = 3800;
	private static final String TRUNCATION_MESSAGE = "\n******* OUTPUT TRUNCATED TO FIRST " + STRING_MAX_SIZE + " CHARACTERS!!! *******";

	private Map<String, String> exitCodeEventsMap = new HashMap<String,String>();
	private boolean throwOnTruncatedVariableBoolean;

	private String activityId;
	private String executionId;
	private String workerId;

	private static final long PROCESS_ENGINE_ERROR_RETRY_DELAY = 5000;
	
	private LockedExternalTask task;
	private Date lockedTime;

	private enum Resolution {
		HANDLE_FAILURE,
		COMPLETE,
		BPMN_ERROR
	}

	public CwsExternalTaskThread(
			RuntimeService runtimeService,
			ExternalTaskService externalTaskService,
			WorkerService workerService,
			LockedExternalTask task,
			Date lockedTime) {
		
		System.out.println("CwsExternalTaskThread constructor...");
		
		this.externalTaskService = externalTaskService;
		this.runtimeService = runtimeService;
		this.workerService = workerService;
		this.task = task;
		this.lockedTime = lockedTime;
	}

	@Override
	public void run() {
		
		log.debug("Started CwsExternalTaskService run thread for external task id: " + task.getId());

		try {
			runTask();
		}
		catch (Throwable t) {

			String msg = "Unknown or uncaught error occured!  Details: " + t.getMessage();

			log.error("Error: " + msg, t);
			
			// Create an incident
			handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), msg, t.toString(), 0, 0L);
		}
	}
	
	
	private void runTask() {

        workerId = workerService.getWorkerId();
		activityId = task.getActivityId();

		log.setProcInstanceId(task.getProcessInstanceId());
		log.setProcDefKey(task.getProcessDefinitionKey());
		log.setActivityInstanceId(task.getActivityInstanceId());

		CmdLineInputFields cmdInputFields = new CmdLineInputFields();
		CmdLineOutputFields cmdOutputFields = new CmdLineOutputFields();

		try {
			if (task.getVariables().containsKey(activityId + "_errorMsg")) {

				String errorMsg = (String)task.getVariables().get(activityId + "_errorMsg");

				// Create an incident
				handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), errorMsg, "", 0, 0L);

				// Don't do anything else with this task -- go to the next one.
				return;
			}
			else {
				String inputFieldName = activityId + "_in";

				if (!task.getVariables().containsKey(inputFieldName)) {
					throw new Exception("Task does not contain the input fields variable.");
				}

				executionId = task.getExecutionId();
				if (executionId == null) {
					log.error("executionId for task: " + task + " was null! Possible instance of https://jira.camunda.com/browse/CAM-10750. " +
							"This hopefully will be fixed in Camunda 7.14");
				}

				// Get input fields as json and convert to CmdLineInputFields object
				SpinJsonNode jsonNode = (SpinJsonNode)task.getVariables().get(inputFieldName);
				cmdInputFields = new Gson().fromJson(jsonNode.toString(), CmdLineInputFields.class);

				throwOnTruncatedVariableBoolean = cmdInputFields.throwOnTruncatedVariable;

				exitCodeEventsMap.clear();
				String[] exitCodeMapArray = cmdInputFields.exitCodeEvents.split(",");
				for (String exitCodeMap : exitCodeMapArray) {

					String[] keyVal = exitCodeMap.split("=");

					exitCodeEventsMap.put(keyVal[0], keyVal[1]);
				}
			}
		}
		catch (Throwable t) {

			// See if we have any more retries for variable lookup failure
			String fieldName = activityId + "_cwsVariableLookupFailRetries";
			
			// Initialize it to 3 if not already set
			int variableLookupRetries = 3;
			
			if (task.getVariables().containsKey(fieldName)) {

				// It does exist, so read it in
				variableLookupRetries = Integer.parseInt((String)task.getVariables().get(fieldName));
			}

			log.warn("Error: Failed to retrieve input variables from execution listener. " + variableLookupRetries + " retries remaining.");

			if (variableLookupRetries > 0) {
				
				// Decrement variable lookup retries
				variableLookupRetries--;

				// Update in database
				setOutputVariable(fieldName, variableLookupRetries + "");

				// Allow for this task to be fetched again and wait for the listener to set the input variables
				//
				// Get retries for this task
				int taskRetries = getAndIncrementRetriesForTask(task, cmdInputFields.retries);

				// Retry this external task
				handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), "Retrying due to variable lookup failure...", t.toString(), taskRetries, 3000L);
			}
			else
			{
				// We got an error and failed to retry this task 3 times, indicating that a critical failure caused the
				// variables to never get set. An incident can now be reasonably thrown.

				String msg = "Failed to retrieve and/or parse input variables.  Details: " + t.getMessage();
				log.error("Error: " + msg, t);

				// Create an incident
				handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), msg, t.toString(), 0, 0L);
			}

			// Don't do anything else with this task -- go to the next one.
			return;
		}

		try {
			setOutputVariable("lockedTime", lockedTime);
			cmdOutputFields.lockedTime = lockedTime;

			boolean success = executeTask(cmdInputFields, cmdOutputFields);

			if (success) {
				log.debug("CmdLine Task completed successfully!");
			}
			else {
				log.debug("CmdLine Task completed unsuccessfully.");
			}

			handleCamundaApiCall(Resolution.COMPLETE, task.getId(), "", "", 0, 0L);
		}
		catch (BpmnError e) {

			// BpmnError occurred, retry or signal a Bpmn Error
			//
			int taskRetries = getAndDecrementRetriesForTask(task, cmdInputFields.retries);

			if (taskRetries > 0) {

				log.warn("handleFailure: (BpmnError: " + e.getMessage() + ") (retries remaining: " + taskRetries + ") (retrying in: " + cmdInputFields.retryDelay + "ms)", e);

				// Do retry
				handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), "Retrying...", e.toString(), taskRetries, cmdInputFields.retryDelay);
			}
			else {
				// Create Bpmn Error
				handleCamundaApiCall(Resolution.BPMN_ERROR, task.getId(), e.getErrorCode(), "", 0, 0L);
			}
		}
		catch (OptimisticLockingException e) {

			int taskRetries = getAndIncrementRetriesForTask(task, cmdInputFields.retries);

			log.error("Error: Optimistic locking exception occurred! Retrying this job.", e);

			// Since OptimisticLockingException is recoverable, wait a short while then retry this external task.
			handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), "Retrying due to OptimisticLockingException...", e.toString(), taskRetries, PROCESS_ENGINE_ERROR_RETRY_DELAY);
		}
		catch (BatchExecutorException e) {
			// Retry database exceptions such as foreign key constraint failures, as they are typically recoverable

			int taskRetries = getAndIncrementRetriesForTask(task, cmdInputFields.retries);

			log.error("Error: Foreign key exception occurred! Retrying this job.", e);

			// Since Foreign Key Exception is recoverable, wait a short while then retry this external task.
			handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), "Retrying due to Foreign Key Exception...", e.toString(), taskRetries, PROCESS_ENGINE_ERROR_RETRY_DELAY);
		}
		catch (ProcessEngineException e) {
			// These are typically issues with the database which are fixed by retrying the job

			int taskRetries = getAndIncrementRetriesForTask(task, cmdInputFields.retries);

			log.error("Error: Process engine exception occurred! Retrying this job. Caused by: " + e.getClass().getCanonicalName(), e);

			String msg = "Retrying job due to process engine exception of type " + e.getClass().getCanonicalName();

			// Process engine errors are typically due to database timing issues, so they are recoverable
			handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), msg, e.toString(), taskRetries, PROCESS_ENGINE_ERROR_RETRY_DELAY);
		}
		catch (Throwable t) {

			String msg = "Failed to complete task.  Details: " + t.getMessage();

			log.error("Error: " + msg, t);

			// Create an incident
			handleCamundaApiCall(Resolution.HANDLE_FAILURE, task.getId(), msg, t.toString(), 0, 0L);
		}
	}

	/**
	 * Get the number of retries for this task plus one (used for retrying a task due to process engine errors)
	 */
	private int getAndIncrementRetriesForTask(LockedExternalTask task, int defaultRetries) {
		// Get the retries for this task
		Integer taskRetries = task.getRetries();

		if (taskRetries == null) {
			// task has not retried yet, initialize retries from "retries" input field
			taskRetries = defaultRetries;
		}

		return ++taskRetries;
	}

	/**
	 * Get the number of retries for this task minus one (used for retrying a task due to business errors)
	 */
	private int getAndDecrementRetriesForTask(LockedExternalTask task, int defaultRetries) {
		// Get the retries for this task
		Integer taskRetries = task.getRetries();

		if (taskRetries == null) {
			// task has not retried yet, initialize retries from "retries" input field
			taskRetries = defaultRetries;
		}

		return --taskRetries;
	}

	/**
	 * Tries to handle failures and avoid exceptions
	 *
	 * Because of a large number of concurrency issues when these methods are called
	 * in rapid succession, we wrap their calls to implement random sleeps after a failure
	 */
	private void handleCamundaApiCall(Resolution resolution, String taskId, String msg, String details, int retries, long delay) {

		// Loop with random sleep until we succeed
		boolean done = false;
		int tries = 50;
		Exception e = new Exception("");

		while (!done && tries > 0) {
			try {
				switch (resolution) {
					case COMPLETE:
						externalTaskService.complete(taskId, workerId);
						break;
					case BPMN_ERROR:
						externalTaskService.handleBpmnError(taskId, workerId, msg);
						break;
					case HANDLE_FAILURE:
						externalTaskService.handleFailure(taskId, workerId, msg, details, retries, delay);
						break;
				}
				// If we get here, we are finished
				done = true;
			}
			catch (Exception _e) {
				// Keep track in case we need to log it
				e = _e;

				// Random sleep to help mitigate concurrency issues
				int millis = ThreadLocalRandom.current().nextInt(100, 500 + 1);

				try {
					Thread.sleep(millis);
				} catch (InterruptedException ie) {
					// Ignore
				}

				// Keep going, as this is likely a recoverable error
				tries--;

				log.warn("Error: Failed Camunda API call " + resolution.toString() + " with exception: " + e.getClass().getCanonicalName());
			}
		}

		// Did we fail 50 times?
		if (tries <= 0) {
			String errMsg = "Tried " + resolution.toString() + " but failed after 50 attempts. " + msg;

			log.error("Error: " + errMsg, e);

			// Create an incident
			externalTaskService.handleFailure(taskId, workerId, errMsg, e.toString(), 0, 0L);
		}
	}

	/**
	 * Class that handles output streams (stdout and stderr) of process
	 * 
	 */
	class CollectingLogOutputStream extends LogOutputStream {
		private final List<String> lines = new LinkedList<String>();
		private int numLinesCollected = 0;

		@Override
		protected void processLine(String line, int level) {
			if (numLinesCollected++ > 1000) {
				if (numLinesCollected == 1001) {
					// only log the warning the first time
					log.warn("only collecting up to 1000 lines.");
				}
				log.debug("LINE: " + line);
			}
			else {
				lines.add((globalOutOrdering.incrementAndGet()) + "__" + line);
				log.debug("LINE: " + line);
			}
		}

		public List<String> getLines() {
			return lines;
		}
	}
	
	
	/**
	 * Function to strip out transient ordering IDs
	 */
	private class StripOrderId<F, T> implements Function<F, T> {
		@SuppressWarnings("unchecked")
		@Override
		public Object apply(Object f) {
			return f.toString().replaceFirst("\\d+__", "");
		}
	}
	
	
	private void setStringVariable(String name, Object value) {
		String stringValue = (String) value;
		if (stringValue.length() > STRING_MAX_SIZE) {
			log.warn("Variable " + name + " is too long ("
					+ stringValue.length() + " chars) and will be truncated.");
			
			// set full value as bytes before truncating
			runtimeService.setVariable(executionId, name + "_bytes", stringValue.getBytes());
			
			// NOTE: You can get the String value back from this _bytes variable
			// by using the ${cws.getVar("foo_bytes")} method..

			// set truncated value as string
			runtimeService.setVariable(executionId, name, stringValue.substring(0, STRING_MAX_SIZE) + TRUNCATION_MESSAGE);
			
			// throw an error, if desired
			if (throwOnTruncatedVariableBoolean) {
				throw new BpmnError(TRUNCATED_ERROR, "Variable " + name + " was truncated.");
			}
		} else {
			runtimeService.setVariable(executionId, name, stringValue); // just set normally
		}
	}
	
	protected void setOutputVariable(String name, Object value) {
		//name = execution.getCurrentActivityId() + "_" + name;
		name = activityId + "_" + name;
		if (value instanceof String) {
			setStringVariable(name, value);
		} else {
			log.trace("Setting non-string process variable [executionId = "
					+ executionId + "]: " + name + " --> " + value);
			runtimeService.setVariable(executionId, name, value);
		}
	}
	
	/**
	 * Looks in the stdout for variables to set
	 */
	private void setStdOutVariables(List<String> stdOutLines) {
		Pattern p = Pattern.compile("^\\d+?__"+CWS_VARS_FROM_STDOUT_REGEX+"$");
		for (String line : stdOutLines) {
			Matcher m = p.matcher(line);
			if (m.matches()) {
				String jsonToParse = m.group(1);
				Map<String, Object> jsonAsMap = new Gson().fromJson(jsonToParse, Map.class);
				log.debug("JSON MAP: " + jsonAsMap);
				for (Entry<String, Object> entry : jsonAsMap.entrySet()) {
					String key = entry.getKey();
					Object val = entry.getValue();
					log.info("STDOUT VARIABLE: [" + key + "-->" + val + "]");
					setOutputVariable(key, val);
				}
			}
		}
	}

	
	private Boolean executeTask(CmdLineInputFields cmdInputFields, CmdLineOutputFields cmdOutputFields) {
		Boolean success = null;
		CollectingLogOutputStream collectingLogOutStream = null;
		CollectingLogOutputStream collectingLogErrStream = null;
		
		try {
			log.info("CmdLineExec (" + cmdInputFields.command + ")\nWorkingDir (" + cmdInputFields.workingDir + ")");
			
			// ------------------------------------------------------------------
			// Workaround for issues in Commons-exec where extra quotes are added.
			//  Do parsing of arguments, ourselves, then use addArgument method.
			//   See:
			//   https://commons.apache.org/proper/commons-exec/faq.html
			//
			List<String> cmdTokens = ArgumentTokenizer.tokenize(cmdInputFields.command);
			CommandLine cmdLine = CommandLine.parse(cmdTokens.get(0));
			int i = 0;
			for (String token : cmdTokens) {
				if (i++ == 0) { continue; } // skip main executable
				log.trace("Added token: " + token);
				cmdLine.addArgument(token, false);
			}
			log.debug("Parsed command: '" + cmdInputFields.command + "' into: " + cmdLine);
			
			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
			
			// Don't require process to execute in a certain amount of time
			//
			ExecuteWatchdog watchdog = new ExecuteWatchdog(cmdInputFields.timeout * 1000);		// convert seconds to milliseconds
			
			Executor executor = new DefaultExecutor();
			executor.setWatchdog(watchdog);
			
			collectingLogOutStream = new CollectingLogOutputStream();
			collectingLogErrStream = new CollectingLogOutputStream();
			PumpStreamHandler psh = new PumpStreamHandler(collectingLogOutStream, collectingLogErrStream);
			executor.setStreamHandler(psh);

			// Check if working directory is valid
			if (!Files.exists(Paths.get(cmdInputFields.workingDir))) {
				// Working directory does not exist
				log.error("Problem executing command line: '" + cmdInputFields.command
						+ "', working directory " + cmdInputFields.workingDir + " does not exist.");

				throw new BpmnError(WORKING_DIR_NOT_FOUND_ERROR, "Working directory not found");
			}

			executor.setWorkingDirectory(new File(cmdInputFields.workingDir));
			
			//
			// Set environment variables for execution based on convention that CWS variables
			// that have the prefix "sysenv_" will be used
			//
			
			Map<String, String> env = EnvironmentUtils.getProcEnvironment();
			log.trace("****************** CURRENT ENVIRONMENT = " + env);
			
			Map<String, Object> vars = runtimeService.getVariables(executionId);
			for (Entry<String,Object> varEntry : vars.entrySet()) {
				if (varEntry.getKey().startsWith("sysenv_")) {
					String envVarToSet = varEntry.getKey().substring(7) + "=" + varEntry.getValue();
					log.debug("Adding [" + envVarToSet + "] to execution environment...");
					EnvironmentUtils.addVariableToEnvironment(env, envVarToSet);
				}
			}
			
			// Execute and wait for the process to complete
			//
			long t0 = System.currentTimeMillis();
			executor.execute(cmdLine, env, resultHandler);
			resultHandler.waitFor();
			long t1 = System.currentTimeMillis();
			
			// Get the exit value, log it, and put it in return map
			//
			int exitValue = resultHandler.getExitValue();
			log.info("Command '" + cmdInputFields.command + "' exit code: " + exitValue + "\nRan in " + (t1 - t0) + " ms.");

			// putting redundant names here on purpose to reduce operator error
			setOutputVariable("exitValue", exitValue + "");
			setOutputVariable("exitCode", exitValue + "");
			cmdOutputFields.exitCode = exitValue;
			
			// Set "success" variable based on comparison with successExitValue
			//
			success = false; // false until proven success
			for (String successCode : cmdInputFields.successfulValues.split(",")) {
				success = new Boolean(Integer.parseInt(successCode) == exitValue);
				if (success) {
					break; // found a match, so must be success
				}
			}
			
			if (!success) {
				log.warn("Exit value " + exitValue + " determined to be a FAILURE");
			}
			setOutputVariable("success", success.toString());
			cmdOutputFields.success = success;

			// Detect whether a certain event case applies (based on exit code)
			//
			for (String eventCode : exitCodeEventsMap.keySet()) {
				if (new Boolean(Integer.parseInt(eventCode) == exitValue)) {
					setOutputVariable("event", exitCodeEventsMap.get(eventCode));
					cmdOutputFields.event = exitCodeEventsMap.get(eventCode);
					break; // can only be one event
				}
			}

			// Collect both stdout and stderr into an 'output' variable
			//
			List<String> stdOutLines = collectingLogOutStream.getLines();
			List<String> stdErrLines = collectingLogErrStream.getLines();
			TreeMap<String, String> sortedLines = new TreeMap<String, String>();
			for (String stdOutLine : stdOutLines) {
				sortedLines.put(stdOutLine, stdOutLine);
			}
			for (String stdErrLine : stdErrLines) {
				sortedLines.put(stdErrLine, stdErrLine);
			}

			// Get trimmed output variable
			String outputStr = StringUtils.join(
					Collections2.transform(sortedLines.values(), new StripOrderId<String, String>()), '\n');

			// trim out the cwsVariable section (if present)
			outputStr.replaceAll(CWS_VARS_FROM_STDOUT_REGEX, "");
			setOutputVariable("output", outputStr);

			// Set stdout output into variable
			//
			String stdoutStr = StringUtils.join(Collections2.transform(stdOutLines, new StripOrderId<String, String>()), '\n');
			setOutputVariable("stdout", stdoutStr);
			cmdOutputFields.stdout = stdoutStr;

			// Set stderr output into variable
			//
			String stderrStr = StringUtils.join(Collections2.transform(stdErrLines, new StripOrderId<String, String>()), '\n');
			setOutputVariable("stderr", stderrStr);
			cmdOutputFields.stderr = stderrStr;

			setStdOutVariables(stdOutLines);
			//log.debug("-------- TASK TIME = " +(System.currentTimeMillis()-t0) + " --------------");

			// Write all outputs into one variable
			JsonValue jsonValue = SpinValues.jsonValue(new Gson().toJson(cmdOutputFields)).create();
			setOutputVariable("out", jsonValue);

			// Check if process timed out
			if (watchdog.killedProcess()) {
			     // it was killed on purpose by the watchdog
				throw new BpmnError(TIMEOUT_ERROR, "Execution exceeded timeout limit: " + cmdInputFields.timeout + " sec");
			}
		} catch (BpmnError e) {
			// Pass these along
			throw e;
		} catch (Throwable e) {
			log.error("Problem executing command line: '" + cmdInputFields.command + "'", e);
			throw new BpmnError(UNEXPECTED_ERROR, e.getMessage());

			// What happens to underlying process if it's running??
		} finally {
			try {
				collectingLogOutStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				collectingLogErrStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// set BpmnError due to non-success return code if necessary
		//
		if (cmdInputFields.throwOnFailures && !success) {
			throw new BpmnError(EXECUTION_NOT_SUCCESS, "throwOnFailuresBoolean is true, and the execution was NOT successful.");
		}
		
		return success;
	}
}
