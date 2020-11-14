package jpl.cws.service.camunda;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Camunda-specific engine execution implementation.
 *
 */
public class CamundaExecutionService {
	private static final Logger log = LoggerFactory.getLogger(CamundaExecutionService.class);

	@Autowired private RuntimeService runtimeService;
	@Autowired private TaskService taskService;
	@Autowired private RepositoryService repositoryService;


	public Set<String> listProcessDefKeys() {
		Set<String> processIdSet = new HashSet<String>();
		List<ProcessDefinition> procDefs = repositoryService.createProcessDefinitionQuery().list();
		for (ProcessDefinition procDef : procDefs) {
			String procDefKey = procDef.getKey();
			log.trace("PROC DEF: "+procDefKey+"  deploymentId="+procDef.getDeploymentId());
			processIdSet.add(procDefKey);
		}
		log.info("GOT "+processIdSet.size()+" AVAILABLE PROCESS DEFINITIONS: "+processIdSet);
		return processIdSet;
	}
	
	
	public List<ProcessDefinition> listProcessDefinitions() {
		return repositoryService.createProcessDefinitionQuery().latestVersion().list();
	}


	public String startProcess(String processId) {
		return startProcess(processId, null);
	}


	public String startProcess(String processId, Map<String,Object> processParams) {
		log.info("STARTNG Camunda Process '"+processId+"'...");
		ProcessInstance processInstance = null;
		if (processParams == null) {
			processInstance = runtimeService.startProcessInstanceByKey(processId);
		}
		else {
			processInstance = runtimeService.startProcessInstanceByKey(processId, processParams);
		}
		String instanceId = processInstance.getId();
		if (instanceId == null) {
			log.error("Process instanceId was null!");
		}
		log.info("STARTED Camunda process '"+processId+"' ID = "+instanceId);
		return instanceId;
	}


	public void waitForProcessInstanceToFinish(String instanceId) {
		log.info("Waiting for Camunda process instance '"+instanceId+"' to finish...");
		// Wait for process to run to completion...
		//
		while (true) {
			ProcessInstance processInstance = 
				runtimeService.createProcessInstanceQuery()
					.processInstanceId(instanceId).singleResult();
			if (processInstance == null) {
				log.info("PROCESS ["+instanceId+"] DOES NOT EXIST ANYMORE");
				break;
			}
			else if (processInstance.isEnded()) {
				log.info("PROCESS ["+instanceId+"] ENDED");
				break;
			}
			else {
				log.info("PROCESS["+processInstance.getProcessInstanceId()+"] (suspended="+processInstance.isSuspended()+",  ended="+processInstance.isEnded()+")");
			}
			try { Thread.sleep(2000); } catch (InterruptedException e) { ; }
		}
	}


	public int publishAllFromProcessRepo() {
		return 0;
	}

}

