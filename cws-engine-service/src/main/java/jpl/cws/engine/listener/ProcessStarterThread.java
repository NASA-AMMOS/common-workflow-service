package jpl.cws.engine.listener;

import static java.lang.Thread.sleep;
import static jpl.cws.core.db.SchedulerDbService.CLAIMED_BY_WORKER;
import static jpl.cws.core.db.SchedulerDbService.FAILED_TO_START;
import static jpl.cws.core.db.SchedulerDbService.PENDING;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;

import de.ruedigermoeller.serialization.FSTObjectInput;
import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.engine.WorkerService;

/**
 * Thread responsible for starting a new process instance in the Camunda engine.
 * 
 */
public class ProcessStarterThread implements Runnable {
	
	private Logger log;
	private RepositoryService repositoryService;
	private RuntimeService runtimeService;
	private SchedulerDbService schedulerDbService;
	private WorkerService workerService;
	private Map<String,Object> procReq;
	
	
	ProcessStarterThread(
			RepositoryService repositoryService,
			RuntimeService runtimeService,
			SchedulerDbService schedulerDbService,
			WorkerService workerService,
			Map<String,Object> procReq,
			Logger log) {
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
		this.schedulerDbService = schedulerDbService;
		this.workerService = workerService;
		this.procReq = procReq;
		this.log = log;
	}
	
	
	@Override
	public void run() {
		log.trace("ProcessStarterThread START run()...");
		
		String failStatus = null; // nothing has failed yet
		boolean processStarted = false; // not started yet
		
		String procStartReqUuid = procReq.get("uuid").toString();
		String procDefKey = procReq.get("proc_def_key").toString();
		
		try {
			long t0 = System.currentTimeMillis();

			// Retry for ~2.3 hours if it fails to find pd
			ProcessDefinition pd = null;
			int waitTime = 1;

			while (pd == null && waitTime < 5000) {

				try {
					pd = repositoryService.createProcessDefinitionQuery()
									.processDefinitionKey(procDefKey)
									.latestVersion()
									.singleResult();
				} catch (Throwable t) {
				}

				if (pd == null) {

					log.error("FIND process instance FAILED.  Will retry in " + waitTime  + " seconds.");

					sleep((long)waitTime * 1000);

					waitTime *= 2;  // Double the wait time each try
				}
			}
			
			// PD should not be null
			//
			if (pd == null) {
				failStatus = FAILED_TO_START;
				throw new Exception("no process definition with proc def key of: '" + procDefKey + "'");
			}
			
			// If the process is currently suspended, then don't attempt to start it.
			//
			if (pd.isSuspended()) {
				failStatus = PENDING; // put back to pending, so it can be tried again later (once unsuspended)
				throw new Exception(
					"Process definition with proc def key of: '" + procDefKey + 
					"' is suspended, so not starting!");
			}
			
			// Get process variables as a map
			//
			byte[] procVarsAsBytes = (byte[])procReq.get("proc_variables");
			FSTObjectInput in = new FSTObjectInput(new ByteArrayInputStream(procVarsAsBytes));
			Map<String,Object> procVars = (Map<String,Object>)in.readObject();
			in.close();
			if (procVars == null) {
				procVars = new HashMap<String,Object>();
			}
			if (log.isTraceEnabled()) {
				log.trace("procVars: " + procVars);
			}
			
			// Make sure there is at least one variable set.
			//
			// During parallel task execution under a parallel gateway, there is a potential for an
			// OptimisticLockingException if no variable has been set in the process yet.
			// This is a workaround to avoid this issue, and might as well set the procDefKey..
			//
			// See: https://groups.google.com/forum/#!topic/camunda-bpm-users/8fUx40_PhAs
			//
			procVars.put("procDefKey", procDefKey == null ? "unknown" : procDefKey);
			procVars.put("startedOnWorkerId", workerService.getWorkerId());
			// FUTURE WORK: Condense these into one JSON object (string)  Do in v2.3
			
			String procBusinessKey = procReq.get("proc_business_key").toString();
			if (procBusinessKey == null) {
				log.error("setting procBusinessKey to procStartRequeUuid because it was null.  This should never be null!");
				procBusinessKey = procStartReqUuid;
			}

			// Retry starting Process instance for ~2.3 hours if it fails to start
			String procInstId = null;
			waitTime = 1;

			while (procInstId == null && waitTime < 5000) {

				try {

					// Start process on this engine, and get process instance ID back
					//
					// random number for when process starts. to have a procInstId .. failed and add break to leave while loop
					procInstId = runtimeService.startProcessInstanceByKey(procDefKey, procBusinessKey, procVars).getId();

				} catch (Throwable t) {
				}

				if (procInstId == null) {

					log.error("START process instance FAILED.  Will retry in " + waitTime  + " seconds.");

					sleep((long)waitTime * 1000);

					waitTime *= 2;  // Double the wait time each try
				}
			}

			if (procInstId == null) {
				throw new FatalProcessStartException("Process instanceId was null!");
			}
			
			// process instance started, so update row in database with process instance ID, and worker ID.
			// (the status of the row is already CLAIMED_BY_WORKER).
			//
			schedulerDbService.updateProcInstIdAndStartedByWorker(
					procStartReqUuid,
					workerService.getWorkerId(),
					procInstId);
			
			processStarted = true;
			
			long startTime = System.currentTimeMillis() - t0;
			if (startTime < 50) {
				log.trace("STARTED process instance for proc def '"+procDefKey+"' ID = "+procInstId);
			}
			else {
				log.warn("STARTED process instance for proc def '"+procDefKey+"' ID = "+procInstId + " (took " + startTime + " ms!)");
			}
		}
		catch (Throwable t) {
			log.error("Unexpected Failure while trying to prepare to start, or start process. " +
					"(procStartReqUuid=" + procStartReqUuid +
					", procDefKey=" + procDefKey +
					", processStarted=" + processStarted +
					", failStatus=" + failStatus + ")" +
					". NOT putting request message back on the queue.", t);
			try {
				if (!processStarted) {
//					// The process counter was incremented in anticipation of the
//					// process start succeeding.  However, since the process start
//					// failed, the counter should now be decremented
//					// (the process is not started or active).
//					//
//					workerService.decrementProcCounter(procStartReqUuid, procDefKey, null);
//					
//					// Remove procStartReqUuid from UUID set, since this
//					// process is not started.
//					//
//					workerService.deRegisterProcStartRequUuid(procStartReqUuid);
//					
					// If the process failed to start,
					// it can either go back to the 
					// PENDING (if PD was suspended) or 
					// FAILED_TO_START (if unexpected error) status.
					//
					schedulerDbService.updateProcInstRowStatus(
							procStartReqUuid,
							CLAIMED_BY_WORKER,
							failStatus == null ? FAILED_TO_START : failStatus,
							"Unexpected Failure: " + t.getMessage(),
							true);
					
					workerService.syncCounters("FAILED TO START PROCESS");
					
				}
			} catch (Exception e) {
				log.error("problem trying to update DB row", e);
			}
		}
		log.trace("ProcessStarterThread END run()...");
	}

}
