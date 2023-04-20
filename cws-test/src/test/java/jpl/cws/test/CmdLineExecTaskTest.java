package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;
import static org.junit.Assert.*;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.spin.json.SpinJsonNode;
import org.junit.*;

/**
 * Tests related to EmailTask
 * 
 */
public class CmdLineExecTaskTest extends CwsTestBase {

	@Rule
	public ProcessEngineRule processEngineRule = new ProcessEngineRule();

	@Before
	// deciding where to ultimately put the jUnit integration
	public void setUp() {
		// MockitoAnnotations.initMocks(this);
	}

	@After
	public void tearDown() {
		Mocks.reset();
	}

	/**
	 * Tests BPMN process that executes a command line program
	 * 
	 */
	@Test
	@Deployment(resources = { "bpmn/test_cmdlineexec_task.bpmn" })
	public void testCase1() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_cmdlineexec_task",
				withVariables("command", "ls"));
		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		System.out.println(vars);
		assertTrue(vars.containsKey("ServiceTask_1_out"));
		SpinJsonNode jsonNode = (SpinJsonNode) vars.get("ServiceTask_1_out");
		String jsonString = jsonNode.toString();
		JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

		assertTrue(jsonObject.has("stdout"));
		assertTrue(jsonObject.has("stderr"));
		claimAndCompleteUserTask(processInstance, "UserTask_1");
	}

	/**
	 * 
	 */
	@Test
	@Deployment(resources = { "bpmn/test_cmdlineexec_task.bpmn" })
	public void testShortStdout() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_cmdlineexec_task",
				withVariables("command", "./src/test/resources/cmd_short_stdout.sh"));
		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		System.out.println(vars);
		assertTrue(vars.containsKey("ServiceTask_1_out"));
		SpinJsonNode jsonNode = (SpinJsonNode) vars.get("ServiceTask_1_out");
		String jsonString = jsonNode.toString();
		JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

		assertTrue(jsonObject.has("stdout"));
		assertTrue(jsonObject.has("stderr"));
		claimAndCompleteUserTask(processInstance, "UserTask_1");
	}

	/**
	 * 
	 */
	@Test
	@Deployment(resources = { "bpmn/test_cmdlineexec_task.bpmn" })
	public void testLongStdout() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_cmdlineexec_task",
				withVariables("command", "./src/test/resources/cmd_long_stdout.sh"));
		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		System.out.println(vars);
		assertTrue(vars.containsKey("ServiceTask_1_out"));
		SpinJsonNode jsonNode = (SpinJsonNode) vars.get("ServiceTask_1_out");
		String jsonString = jsonNode.toString();
		JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

		assertTrue(jsonObject.has("stdout"));
		assertTrue(jsonObject.has("stderr"));
		claimAndCompleteUserTask(processInstance, "UserTask_1");

		assertThat(processInstance).isEnded();
	}

	/**
	 * 
	 */
	@Test
	@Deployment(resources = { "bpmn/test_cmdlineexec_task.bpmn" })
	public void testReallyLongStdout() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_cmdlineexec_task",
				withVariables("command", "./src/test/resources/cmd_really_long_stdout.sh"));
		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		System.out.println(vars);
		assertTrue(vars.containsKey("ServiceTask_1_out"));
		SpinJsonNode jsonNode = (SpinJsonNode) vars.get("ServiceTask_1_out");
		String jsonString = jsonNode.toString();
		JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

		assertTrue(jsonObject.has("stdout"));
		assertTrue(jsonObject.has("stderr"));
		assertTrue(jsonObject.get("stdout").getAsString()
				.endsWith("t234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"));

		assertEquals(30000, jsonObject.get("stdout").getAsString().replaceAll("\n", "").length());

		assertEquals(0, jsonObject.get("stderr").getAsString().length());

		// get value stored as bytes
		String fromBytes = new String(jsonObject.get("stdout").getAsString().getBytes());
		System.out.println("FROM BYTES: " + fromBytes);
		System.out.println(fromBytes.length());
		assertEquals(30299, fromBytes.length());
		
		claimAndCompleteUserTask(processInstance, "UserTask_1");

		assertThat(processInstance).isEnded();
	}

	/**
	 * 
	 */
	@Test
	@Deployment(resources = { "bpmn/test_cmdlineexec_task.bpmn" })
	public void testStderr() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_cmdlineexec_task",
				withVariables("command", "./src/test/resources/cmd_stderr.sh"));

		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		assertTrue(vars.containsKey("ServiceTask_1_out"));
		SpinJsonNode jsonNode = (SpinJsonNode) vars.get("ServiceTask_1_out");
		String jsonString = jsonNode.toString();
		JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

		assertTrue(jsonObject.has("stdout"));
		assertTrue(jsonObject.has("stderr"));
		assertTrue(jsonObject.get("stdout").toString().contains("OUT1"));
		assertFalse(jsonObject.get("stdout").toString().contains("ERR3"));
		assertFalse(jsonObject.get("stderr").toString().contains("OUT1"));
		assertTrue(jsonObject.get("stderr").toString().contains("ERR3"));

		claimAndCompleteUserTask(processInstance, "UserTask_1");
	}
	
	
	/**
	 * 
	 */
	@Test
	@Deployment(resources = { "bpmn/test_cmdlineexec_task.bpmn" })
	public void testStdoutVars() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_cmdlineexec_task",
				withVariables("command", "./src/test/resources/cmd_stdout_vars.sh"));

		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		System.out.println(vars);
		assertTrue(vars.containsKey("ServiceTask_1_out"));
		SpinJsonNode jsonNode = (SpinJsonNode) vars.get("ServiceTask_1_out");
		String jsonString = jsonNode.toString();
		JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

		assertTrue(jsonObject.has("stdout"));
		assertTrue(jsonObject.has("stderr"));
		assertTrue(vars.containsKey("ServiceTask_1_var1"));
		assertTrue(vars.containsKey("ServiceTask_1_var2"));
		assertTrue(vars.containsKey("ServiceTask_1_var3"));
		assertTrue(vars.get("ServiceTask_1_var3").toString().contains("aa,bb,cccccc,ddddddd"));
		
		claimAndCompleteUserTask(processInstance, "UserTask_1");

		assertThat(processInstance).isEnded();
	}

}