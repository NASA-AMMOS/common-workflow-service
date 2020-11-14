package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;

import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests related to RestGetTask
 * 
 */
public class RestPostTaskTest extends CwsTestBase {
	private static final Logger log = LoggerFactory.getLogger(RestPostTaskTest.class);

	private HttpServer server;

	@Rule
	public ProcessEngineRule processEngineRule = new ProcessEngineRule();

	@Before
	public void setUp() throws Exception {
		// Setup a HTTP server that will receive REST calls
		// during the lifetime of these tests.
		//
		server = HttpServer.createSimpleServer(null, 9999);
		server.getServerConfiguration().addHttpHandler(new HttpHandler() {
			public void service(Request request, Response response) throws Exception {
				// Only accept POST requests
				if (!request.getMethod().getMethodString().equals("POST")) {
					response.setStatus(500);
					return;
				}
				response.setContentType("text/plain");
				String resp = "bar";
				response.setContentLength(resp.length());
				response.getWriter().write(resp);
			}
		}, "/foo");

		server.start();
	}

	@After
	public void tearDown() throws Exception {
		server.shutdownNow();
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