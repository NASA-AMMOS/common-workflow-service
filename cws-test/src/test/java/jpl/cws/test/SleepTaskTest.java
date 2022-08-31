package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.*;

/**
 * Tests related to EmailTask
 * 
 */
public class SleepTaskTest {
	
	
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
	 * Tests BPMN process that sleeps for 1 second, and that is synchronous.
	 * 
	 */
	@Test
	@Deployment(resources = {"bpmn/test_sleep_task.bpmn"})
	public void testSyncProcessWithSleep() {
		try {
			long t0 = System.currentTimeMillis();
			System.out.println("STARING PROCESS INSTANCE FOR : test_sleep_task.bpmn");
			ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_sleep_task");
			long t1 = System.currentTimeMillis();
			System.out.println("************************** "+(t1-t0));
			assertTrue((t1-t0)>1000); // best case
			assertTrue((t1-t0)<6000); // worst case
			assertThat(processInstance).isEnded();
		} catch(Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
	}
	
	
	/**
	 * Tests BPMN process that sleeps for 1 second, and that is asynchronous.
	 * Since it is asynchronous, it should return immediately.
	 * 
	 */
	@Test
	@Deployment(resources = {"bpmn/test_sleep_task_async.bpmn"})
	public void testAsyncProcessWithSleep() {
		try {
			long t0 = System.currentTimeMillis();
			ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("test_sleep_task_async");
			long t1 = System.currentTimeMillis();
			System.out.println("************************** "+(t1-t0));
			assertTrue((t1-t0)<1000); // should be quick since async returns immediately
		} catch(Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
	}

}