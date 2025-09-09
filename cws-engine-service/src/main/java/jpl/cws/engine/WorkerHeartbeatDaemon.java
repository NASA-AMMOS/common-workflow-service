package jpl.cws.engine;

import jpl.cws.core.log.CwsEmailerService;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jpl.cws.core.log.CwsWorkerLoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Heartbeat daemon for workers
 */
@Component
public class WorkerHeartbeatDaemon extends Thread {
    @Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
    @Autowired private WorkerService workerService;
    @Autowired private CwsEmailerService emailerService;

    private Logger log;

    private static final int SLEEP_MS           = 5 * 1000; // 5 seconds
    private static final int MAX_FAILURES       = 20;       // Max retries on fatal error

    private static final String LOCK_DAEMON = WorkerExternalTaskLockDaemon.class.getSimpleName();
    private static final String WORKER_DAEMON = WorkerDaemon.class.getSimpleName();

    private volatile boolean running = true;

    public WorkerHeartbeatDaemon() {
        setName("WorkerHeartbeatDaemon");
        setDaemon(true);  // ensures JVM can exit even if this thread is running
    }

    @PostConstruct
    public void startDaemon() {
        log = cwsWorkerLoggerFactory.getLogger(this.getClass());
        log.info("Starting WorkerHeartbeatDaemon...");
        this.start();
    }

    @Override
    public void run() {
        int failures = 0;

        try {
            while (running) {

                // Sleep in small chunks to respond faster to interrupts
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e) {
                    if (!running) break;  // shutdown requested
                    log.warn("WorkerHeartbeatDaemon interrupted during sleep.");
                    Thread.currentThread().interrupt();
                    break;
                }

                if (!running) break;

                try {
                    Set<String> threadClasses = Thread.getAllStackTraces()
                            .keySet()
                            .stream()
                            .map(thread -> thread.getClass().getSimpleName())
                            .collect(Collectors.toSet());

                    if (threadClasses.contains(LOCK_DAEMON) && threadClasses.contains(WORKER_DAEMON)) {
                        workerService.heartbeat();
                    } else {
                        String msg = "Detected one or more critical daemons down, exiting.\n"
                                + "\t" + WORKER_DAEMON + " is " + (threadClasses.contains(WORKER_DAEMON) ? "UP" : "DOWN") + "\n"
                                + "\t" + LOCK_DAEMON + " is " + (threadClasses.contains(LOCK_DAEMON) ? "UP" : "DOWN");

                        log.error(msg);
                        emailerService.sendNotificationEmails(
                                "CWS Worker Error",
                                "Severe Error!\n\nWorker heartbeat daemon encountered fatal runtime error.\n\nDetails: " + msg
                        );

                        workerService.bringWorkerDown();
                        break;
                    }

                } catch (Throwable t) {
                    log.error("Error in WorkerHeartbeatDaemon, " + (MAX_FAILURES - ++failures) + " retries remaining", t);

                    if (failures >= MAX_FAILURES) {
                        emailerService.sendNotificationEmails(
                                "CWS Worker Error",
                                "Severe Error!\n\nWorker heartbeat daemon encountered fatal runtime error.\n\nDetails: "
                                        + ExceptionUtils.getStackTrace(t)
                        );
                        break;
                    }
                }
            }
        } finally {
            log.warn("WorkerHeartbeatDaemon stopping...");
            cleanupReferences();
        }
    }

    @PreDestroy
    public void stopDaemon() {
        log.info("Shutting down WorkerHeartbeatDaemon...");
        running = false;
        this.interrupt();
    }

    /**
     * Nullify references for GC cleanup.
     */
    private void cleanupReferences() {
        cwsWorkerLoggerFactory = null;
        workerService = null;
        emailerService = null;
    }
}
