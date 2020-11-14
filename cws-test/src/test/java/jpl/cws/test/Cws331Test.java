package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.execute;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.job;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;

import java.util.Map;

import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class Cws331Test extends CwsTestBase {
	private static final Logger log = LoggerFactory.getLogger(Cws331Test.class);

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
	@Deployment(resources = { "bpmn/cws331.bpmn" })
	public void testCase1() {
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("cws331",
				withVariables("foo", "bar"));
		
		log.info("************************************** " + processInstance);
		try {
			Thread.sleep(1100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Map<String, Object> vars = runtimeService().getVariables(processInstance.getProcessInstanceId());
		Assert.assertTrue(vars.containsKey("cws331_success"));
		Assert.assertTrue(vars.get("cws331_success").equals(true));
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertThat(processInstance).isActive();
		Job j = job();
		log.info("********* About to execute job: " + j);
		execute(j); 
		 
		assertThat(processInstance).isEnded();
		
	}

}