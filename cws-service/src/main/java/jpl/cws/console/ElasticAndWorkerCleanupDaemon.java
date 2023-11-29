package jpl.cws.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.log.CwsEmailerService;
import jpl.cws.service.CwsConsoleService;

public class ElasticAndWorkerCleanupDaemon extends Thread {
	private static final Logger log = LoggerFactory.getLogger(ElasticAndWorkerCleanupDaemon.class);
	
	private static final int THIS_THREAD_REPEAT_DELAY = 8 * 60 * 60000;		// 4 hours

	@Autowired private CwsConsoleService cwsConsoleService;
	@Autowired private CwsEmailerService cwsEmailerService;
	
	public void run() {
		
		try {
			
			log.debug("ElasticAndWorkerCleanupDaemon starting...");
			
			while (true) {
	
				try {
					sleep(THIS_THREAD_REPEAT_DELAY);
					
					log.debug("Performing ElasticSearch and Worker Log cleanup...");

					cwsConsoleService.cleanupElasticsearch();
					cwsConsoleService.sendWorkerLogCleanupTopicMessage();
					
				} catch (InterruptedException e) {
					log.warn("ElasticAndWorkerCleanupDaemon interrupted. Must be shutting down..");
					break;
				}
			}
			log.warn("ElasticAndWorkerCleanupDaemon stopping...");
		}
		catch (Throwable e) {
			cwsEmailerService.sendNotificationEmails("ElasticAndWorkerCleanupDaemon Error", "Severe Error!\n\nElastic And Worker Cleanup Daemon run threw an exception. This is an unexpected error and should never happen. Look at logs for more details. You may have to restart CWS.\n\nDetails: " + e.getMessage());
			log.error("ElasticAndWorkerCleanupDaemon stopping...", e);
			
			throw e;
		}
	}
	
}
