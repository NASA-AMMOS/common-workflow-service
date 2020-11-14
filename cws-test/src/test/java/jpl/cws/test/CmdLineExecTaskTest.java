package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
		assertTrue(vars.containsKey("ServiceTask_1_stdout"));
		assertTrue(vars.containsKey("ServiceTask_1_stderr"));
		assertTrue(vars.containsKey("ServiceTask_1_output"));
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
		assertTrue(vars.containsKey("ServiceTask_1_stdout"));
		assertTrue(vars.containsKey("ServiceTask_1_stderr"));
		assertTrue(vars.containsKey("ServiceTask_1_output"));
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
		assertTrue(vars.containsKey("ServiceTask_1_stdout"));
		assertTrue(vars.containsKey("ServiceTask_1_stderr"));
		assertTrue(vars.containsKey("ServiceTask_1_output"));
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
		assertTrue(vars.containsKey("ServiceTask_1_stdout"));
		assertTrue(vars.containsKey("ServiceTask_1_stderr"));
		assertTrue(vars.containsKey("ServiceTask_1_output"));
		assertTrue(((String)vars.get("ServiceTask_1_stdout")).endsWith("******* OUTPUT TRUNCATED TO FIRST 3800 CHARACTERS!!! *******"));
		
		assertTrue(((String)vars.get("ServiceTask_1_stdout")).length() ==
				   ((String)vars.get("ServiceTask_1_output")).length());
		
		assertTrue(((String)vars.get("ServiceTask_1_stderr")).length() == 0);
		
		// Get pre-truncated value back from value stored as bytes
		assertTrue(vars.containsKey("ServiceTask_1_output_bytes"));
		
		String fromBytes = new String((byte[])vars.get("ServiceTask_1_output_bytes"));
		System.out.println("FROM BYTES: " + fromBytes);
		System.out.println(fromBytes.length());
		assertTrue(fromBytes.length() == 30299);
		
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
		assertTrue(vars.containsKey("ServiceTask_1_stdout"));
		assertTrue(vars.containsKey("ServiceTask_1_stderr"));
		assertTrue(vars.get("ServiceTask_1_stdout").toString().contains("OUT1"));
		assertTrue(!vars.get("ServiceTask_1_stdout").toString().contains("ERR3"));
		assertTrue(!vars.get("ServiceTask_1_stderr").toString().contains("OUT1"));
		assertTrue(vars.get("ServiceTask_1_stderr").toString().contains("ERR3"));

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
		assertTrue(vars.containsKey("ServiceTask_1_stdout"));
		assertTrue(vars.containsKey("ServiceTask_1_stderr"));
		assertTrue(vars.containsKey("ServiceTask_1_output"));
		assertTrue(vars.containsKey("ServiceTask_1_var1"));
		assertTrue(vars.containsKey("ServiceTask_1_var2"));
		assertTrue(vars.containsKey("ServiceTask_1_var3"));
		assertTrue(vars.get("ServiceTask_1_var3").toString().contains("aa,bb,cccccc,ddddddd"));
		
		claimAndCompleteUserTask(processInstance, "UserTask_1");

		assertThat(processInstance).isEnded();
	}

}