package jpl.cws.console;

import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.log.CwsEmailerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Session;

public class ExternalTaskDaemon extends Thread {
    private static final Logger log = LoggerFactory.getLogger(HistoryCleanupDaemon.class);

    private static final int ONE_HOUR_MS = 60 * 60 * 1000; // 1 hour

    @Autowired private CwsEmailerService cwsEmailerService;
    @Autowired private SchedulerDbService schedulerDbService;
    @Autowired private JmsTemplate jmsProcessExternalTasksTemplate;

    public void run() {

        try {

            long messageInterval = refreshMessageInterval();
            long oneHourCounter = ONE_HOUR_MS;

            while (true) {

                try {
                    sleep(messageInterval);

                    oneHourCounter -= messageInterval;

                    // Update the interval from database every hour
                    if (oneHourCounter <= 0) {
                        messageInterval = refreshMessageInterval();
                        oneHourCounter = ONE_HOUR_MS;
                    }

                    // Add an external task processing message to the queue, to balance load among workers
                    jmsProcessExternalTasksTemplate.send(Session::createBytesMessage);

                } catch (InterruptedException e) {
                    log.warn("ExternalTaskDaemon interrupted. Must be shutting down..");
                    break;
                }
            }
            log.warn("ExternalTaskDaemon stopping...");
        }
        catch (Throwable e) {
            cwsEmailerService.sendNotificationEmails("ExternalTaskDaemon Error", "Severe Error!\n\nExternal Task Daemon threw an exception. This is an unexpected error and should never happen. Look at logs for more details. You may have to restart CWS.\n\nDetails: " + e.getMessage());
            log.error("ExternalTaskDaemon stopping...", e);

            throw e;
        }
    }

    /**
     * Sets the interval for processExternalTask messages dynamically
     * by querying the database for the number of active workers
     *
     * Delay is determined by the linear relationship 1000 / (n active workers)
     * with minimum of 10ms.
     *
     * @return ms of delay
     */
    private long refreshMessageInterval() {
        int activeWorkers = schedulerDbService.getNumUpWorkers();
        int res = 2000; // Default to once every two seconds

        // Make sure not to divide by zero
        if (activeWorkers > 0) {
            res /= activeWorkers;

            // No less than 10ms
            res = Math.max(res, 10);
        }

        return res;
    }
}
