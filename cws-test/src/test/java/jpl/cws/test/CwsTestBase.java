package jpl.cws.test;

//import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.claim;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.complete;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.task;

import org.camunda.bpm.engine.runtime.ProcessInstance;

public class CwsTestBase {
	
	
	public void claimAndCompleteUserTask(ProcessInstance processInstance, String userTaskKey) {
		// Claim and complete user task, to finish this process instance
		//
		assertThat(processInstance).task().hasDefinitionKey(userTaskKey);
		claim(task(), "fozzie");
		assertThat(task()).isAssignedTo("fozzie");
		complete(task());
		
		// Verify that processInstance has ended
		//
		assertThat(processInstance).isEnded();
	}

}
