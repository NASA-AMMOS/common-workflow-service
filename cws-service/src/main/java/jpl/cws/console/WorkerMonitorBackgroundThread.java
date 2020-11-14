package jpl.cws.console;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.db.SchedulerDbService;

public class WorkerMonitorBackgroundThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(WorkerMonitorBackgroundThread.class);
	
	@Autowired private SchedulerDbService schedulerDbService;
	@Autowired private ExternalTaskService externalTaskService;
	
	private static final int THRESHOLD_MILLIS_FOR_DEAD_WORKER = 60000;
	private static final int THIS_THREAD_REPEAT_DELAY = 30000;
	
	public void run() {
		log.debug("WorkerMonitorBackgroundThread starting...");
		
		while (true) {
			try {
				sleep(THIS_THREAD_REPEAT_DELAY);
				
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
				
				
			} catch (InterruptedException e) {
				log.warn("Interrupted. Must be shutting down..");
				break;
			}
			catch (Throwable e) {
				log.error("Unexpected error occured.  Details: ", e);
				break;
			}
		}
		
		log.debug("WorkerMonitorBackgroundThread stopping...");
	}
	
}
