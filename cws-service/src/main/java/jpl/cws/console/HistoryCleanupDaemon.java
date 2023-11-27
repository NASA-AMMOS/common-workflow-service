package jpl.cws.console;

import org.camunda.bpm.engine.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.log.CwsEmailerService;
import jpl.cws.service.CwsConsoleService;

public class HistoryCleanupDaemon extends Thread {
	private static final Logger log = LoggerFactory.getLogger(HistoryCleanupDaemon.class);
	
	private static final int THIS_THREAD_REPEAT_DELAY = 4 * 60 * 60000;		// 4 hours

	@Autowired private CwsConsoleService cwsConsoleService;
	@Autowired private HistoryService historyService;
	@Autowired private CwsEmailerService cwsEmailerService;
	
	public void run() {
		
		try {
			
			log.debug("HistoryCleanupDaemon starting...");
			
			while (true) {
	
				try {
					sleep(THIS_THREAD_REPEAT_DELAY);
					
					log.debug("Performing History Cleanup...");

					// Database history cleanup
					historyService.cleanUpHistoryAsync(true);

					cwsConsoleService.cleanupElasticsearch();
					cwsConsoleService.sendWorkerLogCleanupTopicMessage();
					
				} catch (InterruptedException e) {
					log.warn("HistoryCleanupDaemon interrupted. Must be shutting down..");
					break;
				}
			}
			log.warn("HistoryCleanupDaemon stopping...");
		}
		catch (Throwable e) {
			cwsEmailerService.sendNotificationEmails("HistoryCleanupDaemon Error", "Severe Error!\n\nHistory Cleanup Daemon run threw an exception. This is an unexpected error and should never happen. Look at logs for more details. You may have to restart CWS.\n\nDetails: " + e.getMessage());
			log.error("HistoryCleanupDaemon stopping...", e);
			
			throw e;
		}
	}
	
}
