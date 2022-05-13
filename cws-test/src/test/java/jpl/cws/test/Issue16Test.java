package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;

import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
@Ignore
public class Issue16Test extends CwsTestBase {
	private static final Logger log = LoggerFactory.getLogger(Issue16Test.class);

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

	@Test
	@Deployment(resources = { "bpmn/issue_16_case1.bpmn" })
	public void testCase1() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("issue_16_case1",
				withVariables("foo", "bar"));

		log.info("************************************** " + processInstance);

		assertThat(processInstance).isEnded();
	}

	@Test
	@Deployment(resources = { "bpmn/test_issue_16_case2.bpmn" })
	public void testCase2() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("issue_16_case2",
				withVariables("foo", "bar"));

		assertThat(processInstance).isStarted();
		assertThat(processInstance).isActive();

		log.info("************************************** " + processInstance);

		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		Assert.assertTrue(vars.containsKey("ServiceTask_1_success"));
		Assert.assertTrue(vars.get("ServiceTask_1_success").equals("false"));

		log.info(runtimeService().getVariables(processInstance.getProcessInstanceId()).toString());

		claimAndCompleteUserTask(processInstance, "UserTask_1");
	}

}