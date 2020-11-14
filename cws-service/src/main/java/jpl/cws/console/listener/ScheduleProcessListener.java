package jpl.cws.console.listener;

import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.scheduler.Scheduler;

/**
 * MessageListener that listens for schedule process instance messages.
 * 
 */
public class ScheduleProcessListener implements MessageListener, InitializingBean {
	@Autowired protected Scheduler cwsScheduler;
	
	private static final Logger log = LoggerFactory.getLogger(ScheduleProcessListener.class);

	public ScheduleProcessListener() { }
	
	@Override
	public void afterPropertiesSet() throws Exception {
	}
	
	
	@Override
	public void onMessage(final Message message) {
		if (message instanceof BytesMessage) {
			try {
				log.debug("AMQ MESSAGE :: schedulePI " + message + " Redelivered?: " + message.getJMSRedelivered());
				String procDefKey = message.getStringProperty("procDefKey");
				Map<String,String> procVariables = (Map<String,String>)message.getObjectProperty("procVariables");
				String procBusinessKey = message.getStringProperty("procBusinessKey");
				String initiationKey = message.getStringProperty("initiationKey");
				int priority = message.getIntProperty("priority");
				
				// Schedule the process
				//
				cwsScheduler.scheduleProcess(
						procDefKey, procVariables, procBusinessKey,
						initiationKey, priority);
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

}