package jpl.cws.engine;

import java.util.Date;
import java.util.List;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.exception.NullValueException;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import jpl.cws.core.log.CwsEmailerService;
import jpl.cws.core.log.CwsWorkerLoggerFactory;

/**
 * External task lock daemon for workers
 * @author ztaylor
 * @author jwood
 */
public class WorkerExternalTaskLockDaemon extends Thread {
    @Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
    @Autowired private CwsEmailerService emailerService;
    @Autowired private ExternalTaskService externalTaskService;

	@Value("${cws.worker.id}") private String workerId;

    private Logger log;

    private static final long SLEEP_MS       = 15 * 60 * 1000L;	// 15 minutes
    private static final long EXPIRATION     = 7  * 60 * 1000L; // 7  minutes
    private static final long LOCK_DURATION  = 10 * 60 * 1000L;	// 10 minutes
    private static final int  MAX_FAILURES   = 2;       		// Max retries on fatal error

    @Override
    public void run() {
        log = cwsWorkerLoggerFactory.getLogger(this.getClass());

        int fatalErrors = 0;

        while (true) {
            try {

                Thread.sleep(SLEEP_MS);

                // Extend lock duration on all locked tasks for this worker
                List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().workerId(workerId).locked().list();

        		for (ExternalTask task : tasks) {

                    // If this task is going to expire in the next 7 minutes
        		    if ((task.getLockExpirationTime().getTime() - (new Date()).getTime()) <= EXPIRATION) {
                        try {
                        	
                            log.debug("WorkerExternalTaskLockDaemon: extendLock: Extending external task lock with id " + task.getId() + " on worker " + workerId + " for 1 hour.");
                            
                            externalTaskService.extendLock(task.getId(), workerId, LOCK_DURATION);

                        } catch (OptimisticLockingException e) {
                            // This happens when another transaction updates the task concurrently (should not happen)
                            log.debug("WorkerExternalTaskLockDaemon: Error: Failed to update lock on external task with id " + task.getId() + " because it was updated concurrently. Details: ", e.getMessage(), e);
                        } catch (NullValueException e) {
                            // This happens when we try to extend the lock on a task which is no longer active
                            log.debug("WorkerExternalTaskLockDaemon: Error: Failed to update lock on external task with id " + task.getId() + " because its execution is no longer active. Details: ", e.getMessage(), e);
                        }
                    }
        		}

                fatalErrors = 0;
            } catch (InterruptedException e) {
                log.warn("WorkerExternalTaskLockDaemon: Error: WorkerExternalTaskLockDaemon interrupted. This is normal if the worker is being shutdown.");
                break;
            } catch (Throwable t) {
                log.error("WorkerExternalTaskLockDaemon: Error: Error while running WorkerExternalTaskLockDaemon, " + (MAX_FAILURES - ++fatalErrors) + " retries remaining", t);

                // Stop the thread and notify admin if this daemon has failed more than MAX_FAILURES times
                if (fatalErrors >= MAX_FAILURES) {
                    emailerService.sendNotificationEmails("CWS Worker Error", "Severe Error!\n\nWorker external task lock extender daemon encountered fatal runtime error on worker " + workerId + ".\n\nDetails: " + t);
                    break;
                }
            }
        }

        log.warn("WorkerExternalTaskLockDaemon: Error: WorkerExternalTaskLockDaemon stopping...");
        cwsWorkerLoggerFactory = null;
        externalTaskService = null;
        emailerService = null;
    }
}
