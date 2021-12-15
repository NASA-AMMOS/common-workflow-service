package jpl.cws.console;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;

import jpl.cws.core.db.SchedulerDbService;

public class WorkerMonitorBackgroundThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(WorkerMonitorBackgroundThread.class);
	
	@Autowired private SchedulerDbService schedulerDbService;
	@Autowired private ExternalTaskService externalTaskService;

	@Value("${cws.num.days.after.to.remove.abandoned.workers}") 	private long numDaysAfterRemoveDeadWorkers;

	private static final int THRESHOLD_MILLIS_FOR_DEAD_WORKER = 60000;
	private static final int THIRTY_SECONDS = 30000;
	
	public void run() {
		log.debug("WorkerMonitorBackgroundThread starting...");

		int failures = 0;
		int maxFailures = 20;
		long failWait = 100; // start with 100 milliseconds to wait in between retries

		while (true) {
			try {
				sleep(THIRTY_SECONDS);
				
				// ---------------------------------
				// CHECK FOR DOWN WORKERS
				// ---------------------------------
				
				// Get workers that are detected to be dead,
				// based on last heartbeat recorded in DB
				List<Map<String,Object>> workersThatWentDown = schedulerDbService.detectDeadWorkers(THRESHOLD_MILLIS_FOR_DEAD_WORKER);
				
				while (!workersThatWentDown.isEmpty()) {
					//
					// Get first worker in List
					//
					Map<String,Object> worker = workersThatWentDown.get(0);
					String workerId = worker.get("id").toString();
					Timestamp lastHeartbeatTime = (Timestamp) worker.get("last_heartbeat_time");
					
					// Make worker stop accepting new jobs
					//
					schedulerDbService.setWorkerAcceptingNew(false, workerId);
					
					// Make worker status = 'down' in the database
					//
					schedulerDbService.updateWorkerStatus(workerId, "down");
					
					// Get locked tasks on down worker
					List<ExternalTask> lockedTasks = externalTaskService.createExternalTaskQuery().locked().workerId(workerId).list();
					
					for (ExternalTask task : lockedTasks) {
						
						log.warn("This worker is down and has locked tasks!  Task: " + task.getActivityId());
					}
					
					log.warn("Detected (and made dead in DB) worker '" + workerId +
							"' (threshold milliseconds since last worker heartbeat is " +
							THRESHOLD_MILLIS_FOR_DEAD_WORKER + ").  Worker last heartbeat = " +
							lastHeartbeatTime);
					
					//
					// Get up to date list, based on current time delta
					//
					workersThatWentDown = schedulerDbService.detectDeadWorkers(THRESHOLD_MILLIS_FOR_DEAD_WORKER);
				}


				// ---------------------------------
				// CHECK FOR DOWN WORKERS & DELETE WORKERS THAT ARE PAST THE ABANDONED WORKER LIMIT "remove_abandoned_workers_after_days"
				// ---------------------------------
				while (!workersThatWentDown.isEmpty()) {
					//
					// Get first worker in List
					//


					Map<String,Object> worker = workersThatWentDown.get(0);
					String workerId = worker.get("id").toString();
					Timestamp lastHeartbeatTime = (Timestamp) worker.get("last_heartbeat_time");
					long currentTime = System.currentTimeMillis();
					long lastHeartbeatTimeMillis = lastHeartbeatTime.getTime();
					long workerDeadTimeMillis = currentTime - lastHeartbeatTimeMillis;

					numDaysAfterRemoveDeadWorkers = numDaysAfterRemoveDeadWorkers * 86400000;


					// Check lastHeartbeatTime against remove_abandoned_workers_after_days value
					if (workerDeadTimeMillis > numDaysAfterRemoveDeadWorkers) {
						// Remove "dead" worker from the database
						//
						schedulerDbService.deleteDeadWorkers(workerId);

						log.warn("Detected (and removed row in DB) worker '" + workerId +
							"' (threshold milliseconds since last worker heartbeat is ");

					}


				}
				
				
				// ---------------------------------
				// CHECK FOR DOWN EXTERNAL WORKERS
				// ---------------------------------
				
				// Get external workers that are detected to be dead,
				// based on last heartbeat recorded in DB
				List<Map<String,Object>> externalWorkersThatWentDown = schedulerDbService.detectDeadExternalWorkers(THRESHOLD_MILLIS_FOR_DEAD_WORKER);
				
				while (!externalWorkersThatWentDown.isEmpty()) {
					//
					// Get first worker in List
					//
					Map<String,Object> worker = externalWorkersThatWentDown.get(0);
					String workerId = worker.get("id").toString();
					Timestamp lastHeartbeatTime = (Timestamp) worker.get("last_heartbeat_time");
					
					// Remove "dead" external worker from the database
					//
					schedulerDbService.deleteDeadExternalWorkers(workerId);
					
					log.warn("Detected (and removed row in DB) external worker '" + workerId +
							"' (threshold milliseconds since last worker heartbeat is " +
							THRESHOLD_MILLIS_FOR_DEAD_WORKER + ").  Worker last heartbeat = " +
							lastHeartbeatTime);
					
					//
					// Get up to date list, based on current time delta
					//
					externalWorkersThatWentDown = schedulerDbService.detectDeadExternalWorkers(THRESHOLD_MILLIS_FOR_DEAD_WORKER);
				}

				// Successful thread iteration, so reset failure variables back to nominal
				failures = 0;
				failWait = 100;
				
			} catch (InterruptedException e) {
				log.warn("Interrupted. Must be shutting down..");
				break; // break out of loop to stop this thread
			}
			catch (Throwable e) {
				failures++;
				if (failures > maxFailures) {
					log.error("Unexpected error occurred. Exiting WorkerMonitorBackgroundThread, as there have already been " +
							failures + " failures. Details: ", e);
					break; // break out of loop to abort this thread
				}
				else {

					// wait, before trying again
					try {
						log.warn("Unexpected error occurred.  Waiting " + failWait + " milliseconds, before trying next attempt (" +
								failures + "/" + maxFailures+") ...", e);
						Thread.sleep(failWait);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}

					failWait = failWait * 2; // update exponential backoff wait time
				}
			}
		} // end while true
		
		log.debug("WorkerMonitorBackgroundThread stopping...");
	}
	
}
