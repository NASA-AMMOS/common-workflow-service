package jpl.cws.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.service.ProcessService;

public class ProcessStatusDaemon extends Thread {
	private static final Logger log = LoggerFactory.getLogger(ProcessStatusDaemon.class);
	
	@Autowired private ProcessService cwsProcessService;
	
	public void run() {
		long i = 0;
		while (true) {
			if (++i % 100 == 0) {
				log.debug("ProcessStatusDaemon running (iter = " + i + ")...");
				if (i >= 1000000) { i = 0; }
			}
			
			try {
				sleep(120000);
				
				cwsProcessService.sendProcEventTopicMessageWithRetries(
						null,
						null,
						null,
						null, // deploymentId
						"sync");
				
			} catch (InterruptedException e) {
				log.warn("ProcessStatusDaemon interrupted. Must be shutting down..");
				break;
			}
		}
		log.debug("ProcessStatusDaemon stopping...");
	}
	
}
