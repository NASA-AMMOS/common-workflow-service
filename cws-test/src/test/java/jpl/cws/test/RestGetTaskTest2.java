package jpl.cws.test;

import static org.junit.Assert.assertTrue;
import jpl.cws.task.RestGetTask;
import jpl.cws.task.TestDelegateExecution;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class RestGetTaskTest2 {

	private static final String VAR_PREFIX = TestDelegateExecution.VAR_PREFIX;


	private HttpServer server;

	@Before
	public void setUp() throws Exception {
		// Setup a HTTP server that will receive REST calls
		// during the lifetime of these tests.
		//
		server = HttpServer.createSimpleServer(null, 9999);
		server.getServerConfiguration().addHttpHandler(new HttpHandler() {
			public void service(Request request, Response response) throws Exception {
				response.setContentType("text/plain");
				String resp = "bar";
				response.setContentLength(resp.length());
				response.getWriter().write(resp);
			}
		}, "/foo");

		server.start();
	}
	
	@Test
	public void httpbinTest() {
		RestGetTask task = new RestGetTask();
		
		task.setUrl(new FixedValue("http://localhost:9999/foo"));
		
		DelegateExecution execution = new TestDelegateExecution();
		task.execute(execution);

		assertTrue("TaskResult httpStatusCode variable unexpected value",
				execution.getVariable(VAR_PREFIX + "httpStatusCode").equals("301"));
		System.out.println("RESPONSE:\n" + execution.getVariable(VAR_PREFIX + "response"));
	}
}
