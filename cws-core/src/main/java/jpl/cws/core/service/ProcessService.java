package jpl.cws.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.log.CwsEmailerService;

public class ProcessService {

	private static final Logger log = LoggerFactory.getLogger(ProcessService.class);
	
	@Autowired private SchedulerDbService schedulerDbService;
	@Autowired private RepositoryService repositoryService;
	@Autowired
	@Qualifier("jmsProcEventTopicTemplate")
	private JmsTemplate jmsProcEventTopicTemplate;
	
	@Autowired
	@Qualifier("jmsScheduleProcessTopicTemplate")
	private JmsTemplate jmsScheduleProcessTopicTemplate;
	
	@Autowired private CwsEmailerService cwsEmailerService;

	/**
	 * 
	 */
	public Map<String,Object> getProcInstStatusMap(String uuidOrProcInstId) {
		Map<String,Object> cwsProcInstRow = schedulerDbService.getProcInstRow(uuidOrProcInstId);
		// FIXME: make this query as part of the below query (getProcInstStatus)
		//        so that multiple queries on the database are not necessary!
		//
		return getProcInstStatusMap(cwsProcInstRow);
	}
	
	
	/**
	 * 
	 */
	public String getProcInstStatus(String uuidOrProcInstId) {
		Map<String,Object> data = getProcInstStatusMap(uuidOrProcInstId);
		return data.get("status").toString();
	}
	
	
	/**
	 * 
	 */
	public Map<String,Object> getProcInstStatusMap(Map<String,Object> cwsProcInstRow) {
		Map<String,Object> ret = new HashMap<String,Object>();
		
		String cwsStatus = null;
		String uuid = null;
		String procInstId = null;
		
		if (cwsProcInstRow != null) { // CWS has a row in DB
			ret.put("procDefKey",    cwsProcInstRow.get("proc_def_key"));
			uuid = (String)cwsProcInstRow.get("uuid");
			ret.put("uuid",          cwsProcInstRow.get("uuid"));
			
			procInstId = (String)cwsProcInstRow.get("proc_inst_id");
			ret.put("procInstId",    cwsProcInstRow.get("proc_inst_id"));
			ret.put("errorMessage",  cwsProcInstRow.get("error_message"));
			
			cwsStatus = cwsProcInstRow.get("status").toString();
			ret.put("status",        cwsStatus);
		}
		
		
		if (procInstId != null) {
			//
			// If we have a process instance ID, we can ask Camunda
			// about what they know about the process
			//
			// FIXME: make this query as part of the query that cwsProcInstRow is constructed with so that multiple queries on the database are not necessary!
			//
			List<Map<String,Object>> camundaKnowledge = schedulerDbService.getProcInstStatus(procInstId.toString(), null);
			
			if (!camundaKnowledge.isEmpty()) {
				if (camundaKnowledge.size() > 1) {
					//
					//  Don't consider subprocess ends
					//
					log.error("Unexpected amount of Camunda process knowledge (" + camundaKnowledge.size() + " records found for procInstId " + procInstId + "). Ignoring data!");
					int rec = 0;
					for (Map<String,Object> cm : camundaKnowledge) {
						log.debug("DUMP OF IGNORED DATA (record " + (rec++) + "): " + cm);
					}
				}
				else {
					Map<String,Object> camundaMap = camundaKnowledge.get(0);
					ret.put("startTime",       camundaMap.get("startTime"));
					ret.put("endTime",         camundaMap.get("endTime"));
					ret.put("duration",        camundaMap.get("duration"));
					ret.put("endActivityId",   camundaMap.get("endActivityId"));

					if (ret.get("procDefKey") == null) {
						ret.put("procDefKey", camundaMap.get("procDefKey"));
					}
					if (ret.get("procInstId") == null) {
						ret.put("procInstId", camundaMap.get("procInstId"));
					}
				}
			}
			else {
				log.warn("no Camunda knowledge of procInstId: " + procInstId);
				log.warn(ExceptionUtils.getFullStackTrace(new Throwable()));
			}
		}
		else {
			log.trace("No Camunda knowledge available, since no proc inst ID available yet (uuid = " + uuid + "). " +
					"This may be because no workers are enabled for the process definition, or process has not been started/claimed by a worker.");
		}
		
		return ret;
	}
	
	public String getDeploymentIdForProcDef(String procDefKey) {
		List<ProcessDefinition> procDefs = repositoryService.createProcessDefinitionQuery().latestVersion().list();
		for (ProcessDefinition procDef : procDefs) {
			if (procDef.getKey().equals(procDefKey)) {
				return procDef.getDeploymentId();
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 */
	public boolean isProcDefKeyDeployed(String procDefKey) {

		return getDeploymentIdForProcDef(procDefKey) != null ? true : false;
	}


	/**
	 * 
	 */
	public Boolean isProcDefKeyAcceptingNew(String procDefKey) {
		List<Map<String, Object>> rows = schedulerDbService.getWorkerProcDefRows();
		
	
		for (Map<String, Object> row : rows) {
			String key = row.get("proc_def_key").toString();
	
			if (key.equals(procDefKey)) {
				
				Boolean acceptingNew = (Boolean)row.get("accepting_new");
				
				if (acceptingNew) {
					return true;
				}
			}
			
		}
		
		return false;
	}
	
	
	/**
	 * 
	 */
	public Boolean isProcDefKeyRunning(String procDefKey) {
		List<Map<String, Object>> rows = schedulerDbService.getProcessInstanceStats(null);
		
		for (Map<String, Object> row : rows) {
			String key = row.get("proc_def_key").toString();

			if (key.equals(procDefKey)) {

				String status = row.get("status").toString();
				
				if (status.equals("running")) {

					return true;
				}
			}
		}

		return false;
	}
	
	
	/**
	 * Post to a message broker topic, to notify listeners that a proc event has occurred.
	 * 
	 */
	public void sendProcEventTopicMessageWithRetries(
			final String procInstId,
			final String uuid,
			final String procDefKey,
			final String deploymentId,
			final String eventType) {
		log.trace("sending proc event (" + eventType + ") topic message...");
		
		int numTries = 0;
		long errorSleep = 1000;
		boolean messageSent = false;
		Exception exception = null;
		while (numTries++ < 5) {
			try {
				jmsProcEventTopicTemplate.send(new MessageCreator() {
					public Message createMessage(Session session) throws JMSException {
						BytesMessage msg = session.createBytesMessage();
						msg.setStringProperty("procInstId",   procInstId);
						msg.setStringProperty("uuid",         uuid);
						msg.setStringProperty("procDefKey",   procDefKey);
						msg.setStringProperty("deploymentId", deploymentId);
						msg.setStringProperty("eventType",    eventType);
						return msg;
					}
				});
				messageSent = true;
				break; // no exception
			}
			catch (JmsException e) {
				exception = e;
				log.warn("Error encountered (" + e.getErrorCode() + ": " + e.getMessage() + 
					") while sending a message (procInstId=" + procInstId +
					", uuid=" + uuid +
					", procDefKey=" + procDefKey +
					", deploymentId=" + deploymentId + ", eventType=" + eventType + "). " +
					"Sleeping " + errorSleep + " ms before trying again...");
				try {
					Thread.sleep(errorSleep);
				} catch (InterruptedException e1) {
					log.warn("sleep interrupted", e1);
				}
				errorSleep *= 2; // exponential increase
			}
		} // end while
		
		// Log final error message, if failed to send
		//
		if (!messageSent) {
			cwsEmailerService.sendNotificationEmails("CWS JMS Error", "Severe Error!\n\nCould not access AMQ.\n\nDetails: " + exception.getMessage());

			log.error("JMS message not sent after " + numTries + " retries! (procInstId=" + procInstId +
					", uuid=" + uuid +
					", procDefKey=" + procDefKey +
					", deploymentId=" + deploymentId + ", eventType=" + eventType + ")", exception);
		}
	}
	
	
	/**
	 * Post to a message broker topic,
	 * to notify listeners that a process should be scheduled.
	 * 
	 */
	public void sendProcScheduleMessageWithRetries(
			final String procDefKey,
			final Map<String,String> procVariables,
			final String procBusinessKey,
			final String initiationKey,
			final int priority) {
		log.debug("sending proc schedule message (" + procDefKey + ") topic message...");
		
		int numTries = 0;
		long errorSleep = 1000;
		boolean messageSent = false;
		Exception exception = null;
		while (numTries++ < 5) {
			try {
				jmsScheduleProcessTopicTemplate.send(new MessageCreator() {
					public Message createMessage(Session session) throws JMSException {
						BytesMessage msg = session.createBytesMessage();
						msg.setStringProperty("procDefKey",   procDefKey);
						msg.setObjectProperty("procVariables", procVariables);
						msg.setStringProperty("procBusinessKey",   procBusinessKey);
						msg.setStringProperty("initiationKey",   initiationKey);
						msg.setIntProperty("priority",   priority);
						return msg;
					}
				});
				messageSent = true;
				break; // no exception
			}
			catch (JmsException e) {
				exception = e;
				log.warn("Error encountered (" + e.getErrorCode() + ": " + e.getMessage() + 
					") while sending 'proc schedule' message (procDefKey=" + procDefKey +
					", procVariables=" + procVariables +
					", procBusinessKey=" + procBusinessKey +
					", initiationKey=" + initiationKey + ", priority=" + priority + "). " +
					"Sleeping " + errorSleep + " ms before trying again...");
				try {
					Thread.sleep(errorSleep);
				} catch (InterruptedException e1) {
					log.warn("sleep interrupted", e1);
				}
				errorSleep *= 2; // exponential increase
			}
		} // end while
		
		// Log final error message, if failed to send
		//
		if (!messageSent) {
			cwsEmailerService.sendNotificationEmails("CWS JMS Error", "Severe Error!\n\nCould not access AMQ.\n\nDetails: " + exception.getMessage());

			log.error("JMS message not sent after " + numTries + " retries! " +
			"(procDefKey=" + procDefKey +
			", procVariables=" + procVariables +
			", procBusinessKey=" + procBusinessKey +
			", initiationKey=" + initiationKey +
			", priority=" + priority + "). ",
			exception);
		}
	}

}
