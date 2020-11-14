package jpl.cws.test;

import static org.junit.Assert.assertTrue;
import jpl.cws.task.LogTask;
import jpl.cws.task.TestDelegateExecution;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests related to LogTask
 * 
 */
@RunWith(JUnit4.class)
public class LogTaskTest {

	private static final String VAR_PREFIX = TestDelegateExecution.VAR_PREFIX;

	@Test
	public void simplest() {
		LogTask task = new LogTask();
		// inject expressions
		final String message = "foo bar";
		task.setMessage(new FixedValue(message));
		// execute
		DelegateExecution execution = new TestDelegateExecution();
		task.execute(execution);

		execution.getVariables();

		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable(VAR_PREFIX + "message").equals(message));
	}

}
