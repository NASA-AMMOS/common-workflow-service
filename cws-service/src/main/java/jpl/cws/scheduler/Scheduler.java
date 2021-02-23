package jpl.cws.scheduler;

import static jpl.cws.core.db.SchedulerDbService.FAILED_TO_SCHEDULE;
import static jpl.cws.core.db.SchedulerDbService.PENDING;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Resource;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.util.MultiValueMap;

import de.ruedigermoeller.serialization.FSTObjectOutput;
import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.db.SchedulerJob;

public class Scheduler implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

	@Resource private JmsTemplate processStartReqTopicJmsTemplate;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private SchedulerDbService dbService;
	@Autowired protected SchedulerQueueUtils cwsSchedulerUtils;
	@Autowired private RepositoryService repositoryService;
	
	public static final int DEFAULT_PROCESS_PRIORITY = 10;

	public Scheduler() {
		log.trace("Scheduler constructor...");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.trace("processStartReqTopicJmsTemplate  = " + processStartReqTopicJmsTemplate);
		log.trace("jdbcTemplate = " + jdbcTemplate);
		cwsSchedulerUtils.getAmqClients();
	}

	/**
	 * Validates a request to schedule a process.
	 * 
	 */
	public Map<String,String> validateScheduleRequest(String procDefKey,
			MultiValueMap<String,String> processVariables) throws Exception {
		validateProcDefKey(procDefKey); // throws exception if not valid
		return validateAndGetParameterMap(processVariables); // throws exception if not valid
	}

	/**
	 * Validates procDefKey
	 * 
	 */
	private void validateProcDefKey(String procDefKey) {
		//
		// TODO: validate that procDefKey is valid?
		//
		return; // FIXME: implement me! For now, no checks occur
	}

	/**
	 * Validates process variables
	 * 
	 */
	private Map<String,String> validateAndGetParameterMap(
			MultiValueMap<String,String> processVariables) throws Exception {
		Map<String,String> procVarsMap = new HashMap<String,String>();
		if (processVariables != null && !processVariables.isEmpty()) {

			for (Entry<String, List<String>> entry : processVariables.entrySet()) {
				String key = entry.getKey();
				List<String> values = entry.getValue();
				if (values.size() > 1) {
					log.error("UNEXPECTED MULTIPLE PARAMS WITH SAME KEY: " + key);
					throw new Exception("ERROR: Unexpected multiple parameters with the same key: " + key);
				}
				String val = entry.getValue().get(0);
				try {
					val = URLDecoder.decode(val, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Could not decode param on URL", e);
					throw new Exception("ERROR: Could not decode param '" + val + "'");
				}
				procVarsMap.put(key, val);
			}
		}
		return procVarsMap;
	}
	
	/**
	 * Schedules a process, using the default priority
	 * 
	 */
	public SchedulerJob scheduleProcess(String procDefKey,
			Map<String,String> processVariables,
			String procBusinessKey, String initiationKey) throws Exception {
		return scheduleProcess(procDefKey, processVariables, procBusinessKey, initiationKey,
				DEFAULT_PROCESS_PRIORITY);
	}

	
	/**
	 * Schedules a process
	 * 
	 */
	public SchedulerJob scheduleProcess(String procDefKey,
			Map<String,String> processVariables,
			String procBusinessKey, String initiationKey,
			int priority) throws Exception {
		
		log.trace("Scheduling process definition '" + procDefKey + "' ...");
		log.trace("  with variables=" + processVariables);

		String schedulerJobUuid = null;
		boolean rowCreated = false;
		try {
			// Check if there is a process definition that exists for the procDefKey,
			// and if not, bail out
			//
			if (!isExistingProcDef(procDefKey)) {
				log.warn("Attempting to schedule a non-existent proc def: " + procDefKey);
				throw new Exception("Cannot schedule a non-existent proc def: " + procDefKey);
			}
			
			if (log.isTraceEnabled()) {
				cwsSchedulerUtils.logSchedulerQueues();
				cwsSchedulerUtils.getAmqClients();
			}

			String uuid = UUID.randomUUID().toString();

			// Always ensure that the process businessKey is set.
			// If not specified, then set it the same as the UUID
			//
			if (procBusinessKey == null || procBusinessKey.isEmpty()) {
				procBusinessKey = uuid;
				log.trace("process business key not specified.  Created one automatically: " + uuid);
			}

			Timestamp tsNow = new Timestamp(DateTime.now().getMillis());
			SchedulerJob schedulerJob = new SchedulerJob(
					uuid,
					tsNow, // createdTime
					tsNow, // udpatedTime
					null, null, // worker_id / proc_inst_id -- not assigned yet
					procDefKey, // proc_def_key
					priority, // proc_priority
					processVariables, // proc_variables
					procBusinessKey,
					initiationKey,
					PENDING, // status
					null // no error message
			);

			schedulerJobUuid = schedulerJob.getUuid();

			// Create a cws_sched_worker_proc_inst row in the database.
			// This row will be updated by the Worker, once it accepts the job
			//
			dbService.insertSchedEngineProcInstRow(schedulerJob);
			// TODO: how should we determine whether database insert was a success?
			// What should be done if not success?
			rowCreated = true;
			
			// Serialize the process request data
			final byte[] reqNodeData = createProcReqData(processVariables);

			// Send the message to topic
			//log.debug("sending scheduler topic message... " + processVariables);
			processStartReqTopicJmsTemplate.send(new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					BytesMessage msg = session.createBytesMessage();
					msg.writeBytes(reqNodeData);
					log.trace("wrote bytes to message");
					msg.setIntProperty("numResends", 0);
					log.trace("set numResends = " + msg.getIntProperty("numResends"));
					return msg;
				}
			});
			
			log.debug("Scheduled process definition '" + procDefKey + "' (initiationKey '" + initiationKey + "').");
			return schedulerJob;
			
		} catch (Exception e) {
			if (rowCreated) {
				log.error("failed to schedule process, so setting DB row to '" + FAILED_TO_SCHEDULE + "' state...");
				dbService.updateProcInstRowStatus(schedulerJobUuid, PENDING, FAILED_TO_SCHEDULE, null, true);
			} else {
				log.error("failed to schedule process (DB row not created)");
			}
			throw e;
		}
	}
	
	
	private boolean isExistingProcDef(String procDefKey) {
		ProcessDefinition procDef =  repositoryService.createProcessDefinitionQuery().processDefinitionKey(procDefKey).latestVersion().singleResult();
		log.trace("searched for procDef: " + procDef);
		return procDef != null;
	}
	
	
	/**
	 * Constructs a byte array representing the request process data payload
	 * 
	 */
	private byte[] createProcReqData(Map<String,String> msgPayload)
			throws IOException {
		try (
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				FSTObjectOutput out = new FSTObjectOutput(os);
			)
		{
			out.writeObject(msgPayload);
			return os.toByteArray();
		}
	}

}
