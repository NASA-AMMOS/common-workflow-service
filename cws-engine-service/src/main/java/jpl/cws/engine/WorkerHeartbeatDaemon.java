package jpl.cws.engine;

import jpl.cws.core.log.CwsEmailerService;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.log.CwsWorkerLoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Heartbeat daemon for workers
 * @author ztaylor
 */
public class WorkerHeartbeatDaemon extends Thread {
    @Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
    @Autowired private WorkerService workerService;
    @Autowired private CwsEmailerService emailerService;

    private Logger log;

    private static final int SLEEP_MS           = 5 * 1000; // 5 seconds
    private static final int MAX_FAILURES       = 20;       // Max retries on fatal error

    private static final String LOCK_DAEMON = WorkerExternalTaskLockDaemon.class.getSimpleName();
    private static final String WORKER_DAEMON = WorkerDaemon.class.getSimpleName();

    @Override
    public void run() {
        log = cwsWorkerLoggerFactory.getLogger(this.getClass());

        int failures = 0;

        while (true) {
            try {

                Thread.sleep(SLEEP_MS);

                // Check that daemons are still alive
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                Set<String> threadClasses = threadSet.stream().map(thread -> thread.getClass().getSimpleName()).collect(Collectors.toSet());

                if (threadClasses.contains(LOCK_DAEMON) && threadClasses.contains(WORKER_DAEMON)) {

                    // Send heartbeat
                    workerService.heartbeat();
                }
                else {
                    // One of the daemons is down, report and break
                    String msg = "Detected one or more critical daemons down, exiting.";

                    msg += "\n\t" + WORKER_DAEMON + " is " + (threadClasses.contains(WORKER_DAEMON) ? "UP" : "DOWN");
                    msg += "\n\t" + LOCK_DAEMON + " is " + (threadClasses.contains(LOCK_DAEMON) ? "UP" : "DOWN");

                    log.error(msg);
                    emailerService.sendNotificationEmails("CWS Worker Error", "Severe Error!\n\nWorker heartbeat daemon encountered fatal runtime error.\n\nDetails: " + msg);

                    // Exit
                    workerService.bringWorkerDown();
                    break;
                }

                failures = 0;
            } catch (InterruptedException e) {
                log.warn("WorkerHeartbeatDaemon interrupted. This is normal if the worker is being shutdown.");
                break;
            } catch (Throwable t) {
                log.error("Error while running WorkerHeartbeatDaemon, " + (MAX_FAILURES - ++failures) + " retries remaining", t);

                // Stop the thread and notify admin if this daemon has failed more than 20 times
                if (failures >= MAX_FAILURES) {
                    emailerService.sendNotificationEmails(
                            "CWS Worker Error",
                            "Severe Error!\n\nWorker heartbeat daemon encountered fatal runtime error.\n\nDetails: " + ExceptionUtils.getStackTrace(t));
                    break;
                }
            }
        }

        log.warn("WorkerHeartbeatDaemon stopping...");
        cwsWorkerLoggerFactory = null;
        workerService = null;
    }
}
