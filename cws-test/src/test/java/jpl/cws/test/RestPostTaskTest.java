package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;

import java.util.Map;

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
public class RestPostTaskTest extends CwsTestBase {
	private static final Logger log = LoggerFactory.getLogger(RestPostTaskTest.class);

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
			protected void doPost(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
				response.setContentType("text/plain");
				String resp = "bar";
				response.setContentLength(resp.length());
				response.getWriter().write(resp);
			}
			@Override
			protected void doGet(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
				response.setStatus(500);
			}
		}), "/foo");

		// Return 404 for any other path
		context.addServlet(new ServletHolder(new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
				resp.setStatus(404);
			}
		}), "/*");

		server.start();
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
	}

	/**
	 * Test a standard, valid REST POST
	 */
	@Test
	@Deployment(resources = { "bpmn/test_rest_post_task.bpmn" })
	public void testOk() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
				"test_rest_post_task",
				withVariables("url", "http://localhost:9999/foo", "mediaType", "text/plain", "body", "the payload",
						"throwOnBadResponse", Boolean.FALSE));

		// Get process variables, and verify that response is as expected
		//
		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		log.info("VARIABLES: " + vars);
		Assert.assertTrue(vars.containsKey("Task_1ozy984_httpStatusCode"));
		Assert.assertTrue(vars.get("Task_1ozy984_httpStatusCode").equals("200")); // "OK"

		claimAndCompleteUserTask(processInstance, "UserTask_2");
	}

	/**
	 * Verify that a REST POST to an invalid URL fails
	 * 
	 */
	@Test
	@Deployment(resources = { "bpmn/test_rest_post_task.bpmn" })
	public void testNotFound() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
				"test_rest_post_task",
				withVariables("url", "http://localhost:9999/XXXXXXXXXXX", "mediaType", "text/plain", "body",
						"the payload", "throwOnBadResponse", Boolean.FALSE));

		// Get process variables, and verify that response is as expected
		//
		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		log.info("VARIABLES: " + vars);
		Assert.assertTrue(vars.containsKey("Task_1ozy984_httpStatusCode"));
		Assert.assertTrue(vars.get("Task_1ozy984_httpStatusCode").equals("404")); // "Not Found"

		claimAndCompleteUserTask(processInstance, "UserTask_2");
	}
}