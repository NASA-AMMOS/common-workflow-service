package jpl.cws.engine.listener;

import java.util.Date;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.log.CwsWorkerLoggerFactory;
import jpl.cws.engine.WorkerService;

/**
 * MessageListener that listens for worker log cleanup messages to be published to a topic.
 * 
 * @author jwood
 *
 */
public class WorkerLogCleanupListener implements MessageListener, InitializingBean {
	
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private WorkerService workerService;
	
	private Logger log;
	
	public WorkerLogCleanupListener() { }
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("WorkerLogCleanupListener constructor...");
	}
	
	
	@Override
	public void onMessage(final Message message) {
		if (message instanceof BytesMessage) {
			try {
				log.debug("----------------- GOT WORKER LOG CLEANUP MESSAGE: " + message + " Redelivered?: "+ message.getJMSRedelivered());
				
				int historyDaysToLive = message.getIntProperty("historyDaysToLive");
				
				log.debug("GOT historyDaysToLive: " + historyDaysToLive);
				
				Date keepDate = LocalDate.now().plusDays(-historyDaysToLive).toDate();

				workerService.cleanupLogs(keepDate);
			}
			catch (Exception e) {
				log.error("Exception onMessage", e);
				throw new RuntimeException(e); // FIXME: do we really want to do this?
			}
		}
		else {
			throw new IllegalArgumentException("Message must be of type BytesMessage");
		}
	}
	
	
	//FIXME:  implement a cleanup (via Spring hook??) method that runs:
	//  executor.shutdown();

}