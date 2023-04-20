package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.spin.json.SpinJsonNode;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
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
		// get the value of the map, a JSON object represented by a JacksonJsonNode
		SpinJsonNode jsonNode = (SpinJsonNode) vars.get("ServiceTask_1_out");
		// convert the SpinJsonNode to a string
		String jsonString = jsonNode.toString();
		// parse the JSON string into a JsonObject
		JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

		Assert.assertTrue(vars.containsKey("ServiceTask_1_out"));
		Assert.assertEquals(false, jsonObject.get("success").getAsBoolean());


		log.info(runtimeService().getVariables(processInstance.getProcessInstanceId()).toString());

		claimAndCompleteUserTask(processInstance, "UserTask_1");
	}

}
