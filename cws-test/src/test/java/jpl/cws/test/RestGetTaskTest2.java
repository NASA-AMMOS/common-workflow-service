package jpl.cws.test;

import static org.junit.Assert.assertTrue;
import jpl.cws.task.RestGetTask;
import jpl.cws.task.TestDelegateExecution;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class RestGetTaskTest2 {

	private static final String VAR_PREFIX = TestDelegateExecution.VAR_PREFIX;

	private Server server;

	@Before
	public void setUp() throws Exception {
		// Setup a HTTP server that will receive REST calls
		// during the lifetime of these tests.
		//
		server = new Server(9999);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
				response.setContentType("text/plain");
				String resp = "bar";
				response.setContentLength(resp.length());
				response.getWriter().write(resp);
			}
		}), "/foo");

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
