package jpl.cws.engine.listener;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.log.CwsWorkerLoggerFactory;
import jpl.cws.engine.WorkerService;

/**
 * MessageListener that listens for process definition deployment topic messages.
 * 
 * @author ghollins
 *
 */
public class WorkerConfigChangeListener implements MessageListener, InitializingBean {
	
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private WorkerService workerService;
	
	private Logger log;
	
	public WorkerConfigChangeListener() { }
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("WorkerConfigChangeListener constructor...");
	}
	
	//
	// TOPIC MESSAGE THAT GETS BROADCAST TO ALL WORKERS
	//
	@Override
	public void onMessage(final Message message) {
		if (message instanceof BytesMessage) {
			try {
				
				log.debug("GOT ConfigChange TOPIC MESSAGE: Redelivered?: " + message.getJMSRedelivered());
				workerService.doWorkerConfigChangeActions();
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