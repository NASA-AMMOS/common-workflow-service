package jpl.cws.test;

import static org.junit.Assert.assertTrue;
import jpl.cws.task.SetVariablesTask;
import jpl.cws.task.TestDelegateExecution;

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
public class SetVariablesTaskTest {

	/**
	 * Basic test that simply executes the code.
	 * 
	 * TODO: verify values and other things...
	 */
	@Test
	public void testSections() {
		SetVariablesTask task = new SetVariablesTask();
		task.setSrcPropertiesFile(new FixedValue("./src/test/resources/test_properties.ini"));
		
		DelegateExecution execution = new TestDelegateExecution();
		task.execute(execution);
		
		// Verify some variable values
		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable("default").equals("ok"));
		
		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable("section1_var1").equals("foo"));
		
		assertTrue("TaskResult success variable unexpected success value", execution
				.getVariable("section3_var5").equals("[test1, test2]"));
	}

}
