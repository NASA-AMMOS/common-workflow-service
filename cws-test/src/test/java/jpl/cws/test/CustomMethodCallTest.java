package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.junit.Assert.fail;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the methodology of catching various error at a global level
 * (in this case a sub-process container).
 * 
 */
@Ignore
public class CustomMethodCallTest extends CwsTestBase {
	private static final Logger log = LoggerFactory.getLogger(CustomMethodCallTest.class);
	
	@Rule
	public ProcessEngineRule processEngineRule = new ProcessEngineRule();
	
	@Before // deciding where to ultimately put the jUnit integration
	public void setUp() {
		//MockitoAnnotations.initMocks(this);
	}

	@After
	public void tearDown() {
		Mocks.reset();
	}


	/**
	 * Tests BPMN process that calls a custom method
	 * 
	 * DOES NOT WORK CURRENTLY
	 * 
	 */
	@Test
	@Deployment(resources = {"bpmn/test_custom_method_call.bpmn"})
	public void testCase1() {
		try {
			ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_custom_method_call");
			
			// Verify that error message is accessible outside of sub-process.
			// This also verifies that the sub-process boundary even path was followed.
			//
//			Map<String,Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
//			log.info("VARIABLES: "+vars);
//			Assert.assertTrue(vars.containsKey("ServiceTask_4_unexpectedErrorMessage"));
			//Assert.assertTrue(vars.get("ServiceTask_4_unexpectedErrorMessage").equals("boom!! (simulated NPE)"));
			
			// Claim and complete user task, to finish this process instance
			//
//			claimAndCompleteUserTask(processInstance, "UserTask_1");
			
			assertThat(processInstance).isEnded();
		} catch(Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
	}

}