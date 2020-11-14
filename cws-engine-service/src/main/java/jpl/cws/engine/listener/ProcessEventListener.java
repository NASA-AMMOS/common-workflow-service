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
 * MessageListener that listens for process events.
 * 
 * @author ghollins
 *
 */
public class ProcessEventListener implements MessageListener, InitializingBean {
	
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private WorkerService workerService;
	private static long numMessagesReceived = 0;
	
	private Logger log;
	
	public ProcessEventListener() { }
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("ProcessEventListener constructor...");
	}
	
	
	@Override
	public void onMessage(final Message message) {
		new Thread() {
			public void run() {

				if (message instanceof BytesMessage) {
					try {

						//
						// To mitigate workers doing subsequent actions in a "thundering herd",
						// sleep random amount of time between 0 and 1000 milliseconds here.
						//  See:  https://en.wikipedia.org/wiki/Thundering_herd_problem
						//
						Thread.sleep((long)(Math.random() * 1000));

						if (++numMessagesReceived % 100 == 0) {
							log.debug("ProcessEventListener has received " + numMessagesReceived + " so far.");
							if (numMessagesReceived > (Long.MAX_VALUE - 1000)) {
								log.warn("Rolling over numMessagesReceived variable...");
								numMessagesReceived = 0;
							}
						}

						String eventType = message.getStringProperty("eventType");
						// String procInstId = message.getStringProperty("procInstId");
						String procDefKey = message.getStringProperty("procDefKey");
						String uuid = message.getStringProperty("uuid");
						
						if (log.isTraceEnabled()) {
							log.trace("GOT Process Event MESSAGE: " + eventType + ", " + procDefKey + ", " + uuid);
						}
						
						if (eventType.equals("processEndEventDetected")) {
							boolean endedOnThisWorker = 
								workerService.processEndedActions(procDefKey, uuid);
							if (endedOnThisWorker) {
								workerService.procStartReqAction(procDefKey, "processEndEventDetected message received");
							}
						}
						else if (eventType.equals("sync")) {
							boolean processCounterStateChanged = workerService.syncCounters("received " + eventType + " message");

							if (processCounterStateChanged) {
								log.trace(eventType + " :: state changed");
								
								// If the process counter state changed, then we potentially have more bandwidth to
								// execute more processes of the type that was just completed/failed.
								workerService.procStartReqAction(procDefKey, "sync message received");
							}
						}
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
		}.start();
	}

	//FIXME:  implement a cleanup (via Spring hook??) method that runs:
	//  executor.shutdown();

}