package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import jakarta.ws.rs.client.ClientBuilder;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests related to RestGetTask
 * 
 */
public class RestGetTaskTest extends CwsTestBase {
	private static final Logger log = LoggerFactory.getLogger(RestGetTaskTest.class);

	private Server server;

	@Rule
	public ProcessEngineRule processEngineRule = new ProcessEngineRule();

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

	@After
	public void tearDown() throws Exception {
		server.stop();
	}

	/**
	 * Test to see that the message "Got it!" is sent in the response.
	 */
	@Test
	@Deployment(resources = { "bpmn/test_rest_get_task.bpmn" })
	public void testGetIt() {
		String responseMsg = ClientBuilder.newClient().target("http://localhost:9999/foo").request().get(String.class);

		System.out.println(responseMsg);
		assertEquals("bar", responseMsg);

		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_rest_get_task",
				withVariables(
					"url", "http://localhost:9999/foo",
					"allowInsecure", "true"));

		// Get process variables, and verify that response is as expected
		//
		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		log.info("VARIABLES: " + vars);
		Assert.assertTrue(vars.containsKey("Task_1522pvh_response"));
		Assert.assertTrue(vars.get("Task_1522pvh_response").equals("bar"));

		// Claim and complete user task, to finish this process instance
		//
		claimAndCompleteUserTask(processInstance, "UserTask_1");
	}
}