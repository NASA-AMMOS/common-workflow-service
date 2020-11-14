package jpl.cws.engine.listener;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import jpl.cws.core.log.CwsWorkerLoggerFactory;
import jpl.cws.engine.WorkerService;

/**
 * 
 */
public class ProcessStartRequestListener implements MessageListener, InitializingBean {
	
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private WorkerService workerService;
	
	@Value("${cws.worker.id}") private String workerId;
	
	private Logger log;
	
	private static long numMessagesReceived = 0;
	
	public ProcessStartRequestListener() {
		
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("ProcessRequestListener constructor...");
	}
	
	/**
	 * Worker:  Handling Process Start Requests
	 */
	@Override
	public void onMessage(final Message message) {
		if (message instanceof BytesMessage) {
			
			if (++numMessagesReceived % 100 == 0) {
				log.debug("ProcessStartRequestListener has received " + numMessagesReceived + " so far.");
				if (numMessagesReceived > (Long.MAX_VALUE - 1000)) {
					log.warn("Rolling over numMessagesReceived variable...");
					numMessagesReceived = 0;
				}
			}
			
			try {
				
				log.trace("------- GOT PROC START REQ MESSAGE: Redelivered?: "+ message.getJMSRedelivered());
				
				// TODO: possible get a procDef from message, and pass here?
				workerService.procStartReqAction(null, "process start request message received");
				
			}
			catch (Exception e) {
				log.error("Exception onMessage", e);
			}
			
		}
		else {
			throw new IllegalArgumentException("Message must be of type BytesMessage");
		}
		
	}
	
	
	//FIXME:  implement a cleanup (via Spring hook??) method that runs:
	//  executor.shutdown();

}