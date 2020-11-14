package jpl.cws.engine.listener;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

import jpl.cws.core.log.CwsWorkerLoggerFactory;
import jpl.cws.engine.CwsExternalTaskService;

/**
 * MessageListener that listens for process external tasks requests to be published to a queue.
 * 
 * @author ztaylor
 *
 */
public class ProcessExternalTasksListener implements MessageListener, InitializingBean {

	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private CwsExternalTaskService cwsExternalTaskService;

	@Autowired private JmsTemplate jmsProcessExternalTasksTemplate;

	@Value("${cws.worker.id}") private String workerId;

	private Logger log;

	public ProcessExternalTasksListener() { }

	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("ProcessExternalTasksListener constructor...");
	}

	@Override
	public void onMessage(final Message message) {
		if (message instanceof BytesMessage) {

			if (cwsExternalTaskService.canAcceptMoreWork()) {

				cwsExternalTaskService.processExternalTasks();
			}
		}
		else {
			throw new IllegalArgumentException("Message must be of type BytesMessage");
		}
	}
}