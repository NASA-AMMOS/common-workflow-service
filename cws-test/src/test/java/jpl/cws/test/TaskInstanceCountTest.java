package jpl.cws.test;

//import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.junit.Assert.fail;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that can be run to prove that Camunda instantiates a new
 * instance of each JavaDelegate every time it's run, even if it's
 * the same type.
 * 
 */
public class TaskInstanceCountTest {
	
	
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
	 * Tests BPMN process that sends an email using EmailTask.
	 * 
	 */
	@Test
	@Deployment(resources = {"bpmn/test_task_instance_count.bpmn"})
	public void testCase1() {
		try {
			ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_task_instance_count");
			assertThat(processInstance).isEnded();
		} catch(Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
	}

}