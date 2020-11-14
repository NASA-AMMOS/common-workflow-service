package jpl.cws.engine;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.exception.NullValueException;
import org.camunda.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;
import jpl.cws.core.log.CwsWorkerLoggerFactory;

public class CwsExternalTaskService implements InitializingBean {

	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private ExternalTaskService externalTaskService;
	@Autowired private WorkerService workerService;
	@Autowired private RuntimeService runtimeService;

    @Value("${cws.install.type}") private String installType;
	@Value("${cws.worker.id}") private String workerId;
    @Value("${cws.worker.type}") private String workerType;
	
	private Logger log;
	private int numCores = 0;

	private boolean processing = false;

	private static final Object fetchAndLockSync = new Object();

	private static final long LOCK_DURATION = 60 * 60 * 1000L; 		// Initially lock external task for 1 hour

	final ThreadFactory extTaskThreadFactory = new ThreadFactoryBuilder()
		.setNameFormat("extTaskThread-%d")
		.setDaemon(true)
		.build();
	
	private ExecutorService extTaskThreadPool;
	
	public CwsExternalTaskService() {
		System.out.println("CwsExternalTaskService constructor...");
		
		numCores = Runtime.getRuntime().availableProcessors();
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("CwsExternalTaskService afterPropertiesSet...");

		extTaskThreadPool = Executors.newFixedThreadPool(numCores, extTaskThreadFactory);
	}

	/**
	 * Starts an ExternalTaskThread for a task
	 * @param task the locked external task to start on this worker
	 */
	private void runExternalTask(LockedExternalTask task) {

        Date lockedTime = new Date(task.getLockExpirationTime().getTime() - LOCK_DURATION);

        CwsExternalTaskThread cwsExternalTaskThread = new CwsExternalTaskThread(runtimeService, externalTaskService, workerService, task, lockedTime);

        extTaskThreadPool.execute(cwsExternalTaskThread);
	}

    /**
     * Check if this worker can accept any more work
     */
    public boolean canAcceptMoreWork() {
        try {

            // Don't run external tasks on "console_only" installs or workers with type "run_models_only"
            if (installType.equals("console_only") || workerType.equals("run_models_only")) {
                return false;
            }

            int locked = (int)externalTaskService.createExternalTaskQuery().workerId(workerId).locked().count();

            return locked < numCores;
        } catch (Exception e) {
            // We don't really care if this fails
            return false;
        }
    }

	/**
	 * Process external tasks. Only processes tasks if it is not
     * currently doing so. Otherwise, concurrent calls are ignored.
	 */
	public void processExternalTasks() {

        // Ignore concurrent calls to this function
        if (!processing) {

            // Set the guard to concurrent calls
            processing = true;

            try {
                // Only go in here if there are some not locked tasks
                if (externalTaskService.createExternalTaskQuery().notLocked().count() > 0) {

                    // Number of external tasks already locked for this worker
                    int numLocked = (int) externalTaskService.createExternalTaskQuery().workerId(workerId).locked().count();

                    if (numLocked > 0) {
                        log.debug("processExternalTasks: This worker currently has " + numLocked + " of (max " + numCores + ") locked.");
                    }

                    if (numLocked < numCores) {

                        // Build the query, and ask for numCores - numLocked
                        int requestCount = numCores - numLocked;

                        // Synchronized to avoid race conditions on starting tasks (in case two or more threads
                        // get here before the processing = true guard gets set)
                        synchronized (fetchAndLockSync) {

                            ExternalTaskQueryBuilder fetchAndLock = externalTaskService.fetchAndLock(requestCount, workerId, true);

                            List<String> topics = workerService.getEnabledProcDefKeys();

                            for (String topic : topics) {
                                fetchAndLock.topic(topic, LOCK_DURATION);
                            }

                            List<LockedExternalTask> tasks = fetchAndLock.execute();

                            if (tasks.size() > 0) {
                                StringBuilder sb = new StringBuilder("[");
                                for (LockedExternalTask task : tasks) {
                                    sb.append(task.getTopicName());
                                    sb.append(": ");
                                    sb.append(task.getId());
                                    sb.append(", ");
                                }
                                // Remove trailing comma
                                if (sb.toString().endsWith(", ")) {
                                    sb.setLength(sb.length() - 2);
                                }
                                sb.append("]");

                                log.debug("processExternalTasks: Thread " + Thread.currentThread().getId() + " locked "
                                        + tasks.size() + " tasks on worker " + workerId + ": " + sb.toString());
                            }

                            for (LockedExternalTask task : tasks) {

                                // Spawn a thread to handle this task
                                runExternalTask(task);
                            }
                        }
                    }
                    else {
                        log.debug("processExternalTasks: There are " + numLocked + " locked external tasks now.  Skipping processing...");
                    }
                }
            } catch (NullValueException e) {
                // This is thrown when fetchAndLock tries to lock a task that has been completed (rare concurrency issue, recoverable by skipping)
                log.debug("processExternalTasks: Error: Error locking external tasks, one or more were completed between fetch and lock, and their execution could not be found. Details: ", e.getMessage());
            } catch (OptimisticLockingException e) {
                // This is a general concurrency issue which is recoverable and can usually be ignored
                log.debug("processExternalTasks: Error: Error fetching and locking external tasks due to OptimisticLockingException. Details: ", e.getMessage());
            } catch (NullPointerException e) {
                // An internal Camunda error causes this rarely - likely due to the execution context still being in transaction when it is queried
                log.debug("processExternalTasks: Error: Error fetching and locking external tasks due to NullPointerException. Details: ", e.getMessage());
            } catch (Throwable t) {
                log.debug("processExternalTasks: Error: Unexpected error thrown while trying to fetch and lock external tasks. Details: ", t);
            }

        }

		// Allow more calls to this function
		processing = false;
	}
}