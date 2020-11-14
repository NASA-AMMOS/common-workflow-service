package jpl.cws.engine;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.camunda.bpm.application.ProcessApplicationReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.log.CwsEmailerService;
import jpl.cws.core.log.CwsWorkerLoggerFactory;

public class WorkerDaemon extends Thread {
	
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private WorkerService workerService;
	@Autowired private CwsEmailerService emailerService;
	
	private ProcessApplicationReference processAppRef;

	private Logger log;
	
	private static final int SLEEP_MS    = 5 * 1000;       // 5 seconds
	private static final int TEN_MIN_MS  = 10 * 60 * 1000; // 10 minutes
	private static final int TWO_MIN_MS  = 2  * 60 * 1000; // 2 minutes
	private static final int FIVE_MIN_MS = 5  * 60 * 1000; // 5 minutes
	private static final int ONE_HOUR_MS = 60 * 60 * 1000; // 1 hour
	private static final int MAX_FAILURES     		 = 20; // Max retries on fatal error
	
	@Override
	public void run() {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		
		int tenMinCounter  = TEN_MIN_MS;
		int twoMinCounter  = TWO_MIN_MS;
		int fiveMinCounter = FIVE_MIN_MS;
		int oneHourCounter = ONE_HOUR_MS;

		int failures = 0;

		while (true) {

			try {
				
				Thread.sleep(SLEEP_MS);
				
				tenMinCounter  -= SLEEP_MS;
				fiveMinCounter -= SLEEP_MS;
				twoMinCounter  -= SLEEP_MS;
				oneHourCounter -= SLEEP_MS;
				
				//
				// Log something at least once an hour
				//
				if (oneHourCounter <= 0) {
					//log.debug("WorkerDaemon still alive.");
					oneHourCounter = ONE_HOUR_MS; // reset
				}
				
				if (tenMinCounter <= 0) { // Only run every 10 minutes
					log.debug("WorkerDaemon still alive.");
					
					// NOTE: the below actions technically aren't required if everything
					//       is functioning properly.  This is only a safety fall-back approach
					//
					
					log.trace("WorkerDaemon running fall-back actions...");
					
					workerService.updateProcessCountersAndLimits();
					
					workerService.updateAcceptingProcDefKeys();
					
					workerService.updateProcessAppDeploymentRegistrations(processAppRef);

					tenMinCounter = TEN_MIN_MS; // reset
				}
				
				if (fiveMinCounter <= 0) { // Only run every 5 minutes

					workerService.updateStats();
					
					fiveMinCounter = FIVE_MIN_MS; // reset
				}
				
				if (twoMinCounter <= 0) { // Only run every 2 minutes
					// Fallback to start any processes that haven't been started
					// Technically, this shouldn't be needed...
					//
					workerService.procStartReqAction(null, "worker daemon 2 minute");
					
					twoMinCounter = TWO_MIN_MS; // reset
				}
				
			} catch (InterruptedException e) {
				log.warn("WorkerDaemon interrupted. This is normal if the worker is being shutdown.");
				break;
			}
			catch (Throwable t) {
				log.error("Error while running WorkerDaemon, " + (MAX_FAILURES - ++failures) + " retries remaining", t);

				// Stop the thread and notify admin if this daemon has failed more than 20 times
				if (failures >= MAX_FAILURES) {
					emailerService.sendNotificationEmails(
							"CWS Worker Error",
							"Severe Error!\n\nWorker daemon encountered fatal runtime error.\n\nDetails: " + ExceptionUtils.getStackTrace(t));
					break;
				}
			}
		}
		
		log.warn("WorkerDaemon stopping...");
		processAppRef = null;
		cwsWorkerLoggerFactory = null;
		workerService = null;
	}
	
	public void setProcessApplication(ProcessApplicationReference processAppRef) {
		this.processAppRef = processAppRef;
	}
	
}
