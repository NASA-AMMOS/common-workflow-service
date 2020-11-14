package jpl.cws.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.lang.StringUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.Expression;

import com.google.gson.Gson;

import edu.rice.cs.util.ArgumentTokenizer;
import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Collections2;

/**
 * Built-in task that executes a command-line program.
 * 
 * REQUIRED parameters: -- cmdLine -- successExitValue
 * 
 */
public class CmdLineExecTask extends CwsTask {

	private static final String DEFAULT_CMD_LINE_STRING = "say you failed to specify a command line program";
	private static final String DEFAULT_SUCCESS_EXIT_VALUE = "0";
	
	// throw by default
	private static final boolean DEFAULT_THROW_ON_FAILURES = true;

	// FIXME:  Re-evaluate this number.  Does it wrap? Will it run out?
	private static AtomicInteger globalOutOrdering = new AtomicInteger(1000000);
	private static final String EXECUTION_NOT_SUCCESS = "executionNotSuccess";

	private static final String CWS_VARS_FROM_STDOUT_REGEX = "\\\"?cwsVariables\\\"?\\s*:\\s*(\\{(.+?)\\})";

	private static final String WORKING_DIR_NOT_FOUND_ERROR = "workingDirNotFoundError";
	
	// The command-line program to execute
	private Expression cmdLineExpr;
	private String cmdLineString;
	
	// Comma-separated list of exit values to be considered a "success"
	private Expression successExitValues;
	private String[] successExitValuesSplit;
	
	// If true, then a non-success return code will result in throwing a
	// BpmnError
	private Expression throwOnFailures;
	private Expression exitCodeEvents;
	
	
	private boolean throwOnFailuresBoolean;
	private Map<String, String> exitCodeEventsMap;
	
	private Expression workingDirExpr;
	private String workingDirString;
	
	public CmdLineExecTask() {
		log.trace("CmdLineExecTask constructor...");
	}

	@Override
	public void initParams() throws Exception {
		cmdLineString = getStringParam(cmdLineExpr, "cmdLine", DEFAULT_CMD_LINE_STRING);
		workingDirString = getStringParam(workingDirExpr, "workingDir", System.getProperty("user.dir"));

		successExitValuesSplit = getStringParam(successExitValues, "successExitValues", DEFAULT_SUCCESS_EXIT_VALUE)
				.split(",");
		throwOnFailuresBoolean = getBooleanParam(throwOnFailures, "throwOnFailures", DEFAULT_THROW_ON_FAILURES);
		exitCodeEventsMap = getMapParam(exitCodeEvents, "exitCodeEvents", new HashMap<String, String>());

		// TODO: do some validation here to check whether
		// successExitValues/eventMappings values overlap/conflict
		// Should these be allowed to overlap? (i.e. can be a success, and also
		// throw an event)
	}

	/**
	 * Base implementation of a command-line execution
	 * 
	 */
	@Override
	public void executeTask() {
		Boolean success = null;
		CollectingLogOutputStream collectingLogOutStream = null;
		CollectingLogOutputStream collectingLogErrStream = null;

		try {
			//long t0 = System.currentTimeMillis();
			log.info("CmdLineExec (" + cmdLineString + ")\nWorkingDir (" + workingDirString + ")");
			
			// ------------------------------------------------------------------
			// Workaround for issues in Commons-exec where extra quotes are added.
			//  Do parsing of arguments, ourselves, then use addArgument method.
			//   See:
			//   https://commons.apache.org/proper/commons-exec/faq.html
			//
			List<String> cmdTokens = ArgumentTokenizer.tokenize(cmdLineString);
			CommandLine cmdLine = CommandLine.parse(cmdTokens.get(0));
			int i = 0;
			for (String token : cmdTokens) {
				if (i++ == 0) { continue; } // skip main executable
				log.trace("Added token: " + token);
				cmdLine.addArgument(token, false);
			}
			log.debug("parsed command: '" + cmdLineString + "' into: " + cmdLine);
			
			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
			
			// Don't require process to execute in a certain amount of time
			//
			ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
			
			Executor executor = new DefaultExecutor();
			executor.setWatchdog(watchdog);
			
			collectingLogOutStream = new CollectingLogOutputStream();
			collectingLogErrStream = new CollectingLogOutputStream();
			PumpStreamHandler psh = new PumpStreamHandler(collectingLogOutStream, collectingLogErrStream);
			executor.setStreamHandler(psh);

			// Check if working directory is valid
			if(!Files.exists(Paths.get(workingDirString))) {
				// Working directory does not exist
				log.error("Problem executing command line: '" + cmdLineString
						+ "', working directory " + workingDirString + " does not exist.");

				throw new BpmnError(WORKING_DIR_NOT_FOUND_ERROR, "Working directory not found");
			}

			executor.setWorkingDirectory(new File(workingDirString));
			
			//
			// Set environment variables for execution based on convention that CWS variables
			// that have the prefix "sysenv_" will be used
			//
			Map env = EnvironmentUtils.getProcEnvironment();
			log.trace("****************** CURRENT ENVIRONMENT = " + env);
			for (Entry<String,Object> varEntry : execution.getVariables().entrySet()) {
				if (varEntry.getKey().startsWith("sysenv_")) {
					String envVarToSet = varEntry.getKey().substring(7) + "=" + varEntry.getValue();
					log.debug("adding [" + envVarToSet + "] to execution environment...");
					EnvironmentUtils.addVariableToEnvironment(env, envVarToSet);
				}
			}
			
			// Execute and wait for the process to complete
			//
			log.info("About to execute '" + cmdLine + "'");
			long t0 = System.currentTimeMillis();
			executor.execute(cmdLine, env, resultHandler);
			resultHandler.waitFor();
			long t1 = System.currentTimeMillis();
			
			// Get the exit value, log it, and put it in return map
			//
			int exitValue = resultHandler.getExitValue();
			log.info("Command '" + cmdLineString + "' exit value:" + exitValue + ". Ran in: " + (t1 - t0) + " ms.");
			
			// putting redundant names here on purpose to reduce operator error
			setOutputVariable("exitValue", exitValue + "");
			setOutputVariable("exitCode", exitValue + "");

			// Set "success" variable based on comparison with successExitValue
			//
			success = false; // false until proven success
			for (String successCode : successExitValuesSplit) {
				success = new Boolean(Integer.parseInt(successCode.trim()) == exitValue);
				if (success) {
					break; // found a match, so must be success
				}
			}
			
			if (!success) {
				log.warn("Exit value " + exitValue + " determined to be a FAILURE");
			}
			setOutputVariable("success", success.toString());

			// Detect whether a certain event case applies (based on exit code)
			//
			for (String eventCode : exitCodeEventsMap.keySet()) {
				if (new Boolean(Integer.parseInt(eventCode) == exitValue)) {
					setOutputVariable("event", exitCodeEventsMap.get(eventCode));
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
			setOutputVariable("stdout",
					StringUtils.join(Collections2.transform(stdOutLines, new StripOrderId<String, String>()), '\n'));

			// Set stderr output into variable
			//
			setOutputVariable("stderr",
					StringUtils.join(Collections2.transform(stdErrLines, new StripOrderId<String, String>()), '\n'));

			setStdOutVariables(stdOutLines);
			//log.debug("-------- TASK TIME = " +(System.currentTimeMillis()-t0) + " --------------");
		} catch (BpmnError e) {
			// Pass these along
			throw e;
		} catch (Throwable e) {
			log.error("Problem executing command line: '" + cmdLineString + "'", e);
			throw new BpmnError(UNEXPECTED_ERROR);

			// What happens to underlying process if it's running??
		} finally {
			if (collectingLogOutStream != null) {
				try {
					collectingLogOutStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (collectingLogErrStream != null) {
				try {
					collectingLogErrStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// set BpmnError due to non-success return code if necessary
		//
		if (throwOnFailuresBoolean && !success) {
			throw new BpmnError(EXECUTION_NOT_SUCCESS);
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
				Gson gson = new Gson();
				Map<String, Object> jsonAsMap = gson.fromJson(jsonToParse, Map.class);
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

	/**
	 * Class that handles output streams (stdout and stderr) of process
	 * 
	 */
	class CollectingLogOutputStream extends LogOutputStream {
		private final List<String> lines = new LinkedList<String>();

		@Override
		protected void processLine(String line, int level) {
			lines.add((globalOutOrdering.incrementAndGet()) + "__" + line);
			log.debug("LINE: " + line); // +", level="+level);
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

	public Expression getCmdLine() {
		return cmdLineExpr;
	}

	public void setCmdLine(Expression cmdLine) {
		this.cmdLineExpr = cmdLine;
	}

	public Expression getWorkingDir() {
		return workingDirExpr;
	}

	public void setWorkingDir(Expression workingDir) {
		this.workingDirExpr = workingDir;
	}
	
	public Expression getSuccessExitValues() {
		return successExitValues;
	}

	public void setSuccessExitValues(Expression successExitValues) {
		this.successExitValues = successExitValues;
	}

	public Expression getThrowOnFailures() {
		return throwOnFailures;
	}

	public void setThrowOnFailures(Expression throwOnFailures) {
		this.throwOnFailures = throwOnFailures;
	}

	public Expression getExitCodeEvents() {
		return exitCodeEvents;
	}

	public void setExitCodeEvents(Expression exitCodeEvents) {
		this.exitCodeEvents = exitCodeEvents;
	}
	
	//
	// FOR TESTING PURPOSES ONLY
	//
	public static void main(String args[]) {
	
		CmdLineExecTask cmdTask = new CmdLineExecTask();
		cmdTask.cmdLineString = "/Users/user/dev/temp/args.sh one \"two 2\" 'three ee'";
		cmdTask.successExitValuesSplit = DEFAULT_SUCCESS_EXIT_VALUE.split(",");
		cmdTask.throwOnFailuresBoolean = false;
		cmdTask.exitCodeEventsMap = new HashMap<String,String>();
		cmdTask.executeTask();
	}

}
