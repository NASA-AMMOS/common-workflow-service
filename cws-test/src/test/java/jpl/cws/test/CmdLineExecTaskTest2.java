package jpl.cws.test;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import jpl.cws.task.CmdLineExecTask;
import jpl.cws.task.TestDelegateExecution;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests related to CmdLineExecTask
 * 
 */
@RunWith(JUnit4.class)
public class CmdLineExecTaskTest2 {

	private static final String VAR_PREFIX = TestDelegateExecution.VAR_PREFIX;

	@Test
	public void simplest() {
		CmdLineExecTask task = new CmdLineExecTask();
		task.setCmdLine(new FixedValue("ls"));
		task.setSuccessExitValues(new FixedValue("0"));
		task.setThrowOnFailures(new FixedValue(true));
		task.setThrowOnTruncatedVariable(new FixedValue(false));
		task.setExitCodeEvents(new FixedValue(new HashMap<String, String>()));

		DelegateExecution execution = new TestDelegateExecution();
		task.execute(execution);

		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable(VAR_PREFIX + "success").equals("true"));

		assertTrue("TaskResult success variable unexpected exitValue value",
				execution.getVariable(VAR_PREFIX + "exitValue").equals("0"));

		assertTrue("TaskResult success variable unexpected stdOutLines value",
				((String) execution.getVariable(VAR_PREFIX + "stdout")).contains("pom.xml"));
	}

	@Test
	public void multiSuccessValues() {
		CmdLineExecTask task = new CmdLineExecTask();
		task.setCmdLine(new FixedValue("ls"));
		task.setSuccessExitValues(new FixedValue("1,2,3,0,5,6"));
		task.setThrowOnFailures(new FixedValue(true));
		task.setThrowOnTruncatedVariable(new FixedValue(false));
		task.setExitCodeEvents(new FixedValue(new HashMap<String, String>()));

		DelegateExecution execution = new TestDelegateExecution();
		task.execute(execution);

		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable(VAR_PREFIX + "success").equals("true"));

		assertTrue("TaskResult success variable unexpected exitValue value",
				execution.getVariable(VAR_PREFIX + "exitValue").equals("0"));

		assertTrue("TaskResult success variable unexpected stdOutLines value",
				((String) execution.getVariable(VAR_PREFIX + "stdout")).contains("pom.xml"));
	}

	@Test
	public void badCommand() {
		CmdLineExecTask task = new CmdLineExecTask();
		task.setCmdLine(new FixedValue("NON_EXISTENT_COMMAND"));
		task.setSuccessExitValues(new FixedValue("0"));
		task.setThrowOnFailures(new FixedValue(true));
		task.setThrowOnTruncatedVariable(new FixedValue(false));
		task.setExitCodeEvents(new FixedValue(new HashMap<String, String>()));

		DelegateExecution execution = new TestDelegateExecution();
		boolean bpmnErrorThrown = false;
		try {
			task.execute(execution);
		} catch (BpmnError e) {
			bpmnErrorThrown = true;
		}
		assertTrue(bpmnErrorThrown);

		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable(VAR_PREFIX + "success").equals("false"));

		assertTrue("TaskResult success variable unexpected exitValue value",
				!execution.getVariable(VAR_PREFIX + "exitValue").equals("0"));

		assertTrue("TaskResult success variable unexpected stdOutLines value",
				((String) execution.getVariable(VAR_PREFIX + "stdout")).isEmpty());
	}

	@Test
	public void testCommand1() {
		CmdLineExecTask task = new CmdLineExecTask();
		task.setCmdLine(new FixedValue("./src/test/resources/test_cmd_1.sh"));
		task.setSuccessExitValues(new FixedValue("0"));
		task.setThrowOnFailures(new FixedValue(true));
		task.setThrowOnTruncatedVariable(new FixedValue(false));
		task.setExitCodeEvents(new FixedValue(new HashMap<String, String>()));

		DelegateExecution execution = new TestDelegateExecution();
		boolean bpmnErrorThrown = false;
		try {
			task.execute(execution);
		} catch (BpmnError e) {
			bpmnErrorThrown = true;
		}
		assertTrue(bpmnErrorThrown);

		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable(VAR_PREFIX + "success").equals("false"));

		assertTrue("TaskResult success variable unexpected exitValue value",
				execution.getVariable(VAR_PREFIX + "exitValue").equals("100"));

		assertTrue("TaskResult success variable unexpected stdOutLines value",
				execution.getVariable(VAR_PREFIX + "stdout").equals("one"));

	}

	@Test
	public void testEventMapping1() {
		CmdLineExecTask task = new CmdLineExecTask();
		task.setCmdLine(new FixedValue("./src/test/resources/test_cmd_1.sh"));
		task.setSuccessExitValues(new FixedValue("0"));
		task.setThrowOnFailures(new FixedValue(true));
		task.setThrowOnTruncatedVariable(new FixedValue(false));
		Map<String, String> exitCodeEventMappings = new HashMap<String, String>();
		exitCodeEventMappings.put("0", "EVENT_0");
		exitCodeEventMappings.put("100", "EVENT_100");
		task.setExitCodeEvents(new FixedValue(exitCodeEventMappings));

		DelegateExecution execution = new TestDelegateExecution();
		boolean bpmnErrorThrown = false;
		try {
			task.execute(execution);
		} catch (BpmnError e) {
			bpmnErrorThrown = true;
		}
		assertTrue(bpmnErrorThrown);

		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable(VAR_PREFIX + "success").equals("false"));

		assertTrue("TaskResult success variable unexpected exitValue value",
				execution.getVariable(VAR_PREFIX + "exitValue").equals("100"));

		assertTrue("TaskResult success variable unexpected stdOutLines value",
				execution.getVariable(VAR_PREFIX + "stdout").equals("one"));

		assertTrue("TaskResult success variable unexpected event value", execution.getVariable(VAR_PREFIX + "event")
				.equals("EVENT_100"));
	}

}
