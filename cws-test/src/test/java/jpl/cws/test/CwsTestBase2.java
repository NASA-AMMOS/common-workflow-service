package jpl.cws.test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.claim;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.complete;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.task;

import org.camunda.bpm.engine.runtime.ProcessInstance;

public class CwsTestBase2 {
	
	
	public void claimAndCompleteUserTask(ProcessInstance processInstance, String userTaskKey) {
		// Claim and complete user task, to finish this process instance
		//
		assertThat(processInstance).task().hasDefinitionKey(userTaskKey);
		claim(task(), "fozzie");
		assertThat(task()).isAssignedTo("fozzie");
		complete(task());
	}

//
//	public void assertProcessActive()
//	ProcessDefinition procDef = repositoryService()
//			.createProcessDefinitionQuery()
//			.processDefinitionKey("issue_16_case2")
//			.singleResult();
//	
//	log.info("PROC DEF: "+procDef);
//	List<ProcessInstance> processInstances = 
//			runtimeService().createProcessInstanceQuery()
//				.processDefinitionId(procDef.getId()).list();
//	
//	Assert.assertTrue(processInstances.size() == 1);
//	assertThat(processInstances.iterator().next()).isStarted();
//	assertThat(processInstances.iterator().next()).isActive();
}
