package jpl.cws.test;

import static org.junit.Assert.assertTrue;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.junit.Ignore;
import org.junit.Test;

import jpl.cws.task.RestPostTask;
import jpl.cws.task.TestDelegateExecution;

public class RestPostTaskTest2 {

	private static final String VAR_PREFIX = TestDelegateExecution.VAR_PREFIX;

	@Test
	public void httpbinTest() {
		RestPostTask task = new RestPostTask();
		task.setUrl(new FixedValue("http://httpbin.org/post"));

		DelegateExecution execution = new TestDelegateExecution();
		task.execute(execution);

		assertTrue("TaskResult httpStatusCode variable unexpected value",
				execution.getVariable(VAR_PREFIX + "httpStatusCode").equals("301"));
	}

	@Test
	public void httpbinBodyTest() {
		RestPostTask task = new RestPostTask();
		task.setUrl(new FixedValue("http://httpbin.org/post"));
		task.setBody(new FixedValue("test body"));

		DelegateExecution execution = new TestDelegateExecution();
		task.execute(execution);

		assertTrue("TaskResult httpStatusCode variable unexpected value",
				execution.getVariable(VAR_PREFIX + "httpStatusCode").equals("301"));
		System.out.println(execution.getVariable(VAR_PREFIX + "response"));
	}

	@Test
	public void httpbin404Test() {
		RestPostTask task = new RestPostTask();
		task.setUrl(new FixedValue("http://httpbin.org/asdf"));
		task.setBody(new FixedValue("test body"));
		task.setThrowOnBadResponse(new FixedValue(false));

		DelegateExecution execution = new TestDelegateExecution();
		task.execute(execution);

		assertTrue("TaskResult httpStatusCode variable unexpected value",
				execution.getVariable(VAR_PREFIX + "httpStatusCode").equals("404"));
		System.out.println(execution.getVariable(VAR_PREFIX + "response"));
	}
}
