package jpl.cws.process.initiation.message;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.activemq.command.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import jpl.cws.core.service.SpringApplicationContext;
import jpl.cws.process.initiation.CwsProcessInitiator;

/**
 * Schedules a process for execution when a message arrives.
 * 
 */
public class MessageArrivalInitiator extends CwsProcessInitiator implements MessageListener, InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(MessageArrivalInitiator.class);
	
	private String regex;
	private String xtorRegex;
	
	private DefaultMessageListenerContainer mlc;
	
	public MessageArrivalInitiator() {} // needed by Spring for construction
	
	
	@Override
	public void onMessage(Message message) {
		if (message instanceof BytesMessage) {
			try {
				log.debug("----------------- GOT MESSAGE: "+ message + " Redelivered?: "+ message.getJMSRedelivered());
				String key = message.getStringProperty("key");
				if (Pattern.matches(regex, key)) {
					if (isEnabled()) {
						procVariables.put("messageInitiatorPayload", key);
						setInitiationKey(key);
						scheduleProcess();
					}
				}
			}
			catch (Exception e) {
				log.error("Exception onMessage", e);
				throw new RuntimeException(e); // FIXME: do we really want to do this?
			}
		}
		else {
			throw new IllegalArgumentException("Message must be of type TextMessage");
		}
	}
	
	
	@Override
	public void run() {
		if (!isValid()) {
			log.warn("Not running MessageArrivalInitiator because it's not valid");
			return;
		}
		try {
			while (true) {
				if (!isEnabled()) {
					log.debug("Noticed that this process initiator is now disabled.  Stopping...");
					break;
				}
				Thread.sleep(1000);
			}
		}
		catch (InterruptedException ie) {
			log.info("MessageArrivalInitiator: Thread interrupted, now terminating...");
			cleanUp();
		}
	}
	
	
	@Override
	public MutablePropertyValues getSpecificPropertyValues() {
		MutablePropertyValues propVals = new MutablePropertyValues();
		
		propVals.add("regex", xtorRegex);
		
		return propVals;
	}

	@Override
	public void reapplySpecificProps() {

		setRegex(xtorRegex);
	}
	
	@Override
	public void cleanUp() {
		if (mlc != null) {
			mlc.stop();
			mlc.destroy();
		}
		mlc = null;
	}
	
	
	@Override
	public boolean isValid() {
		// Validates that regex is syntactically correct
		//
		PatternSyntaxException exc = null;
		
		try {
			Pattern.compile(regex);
		}
		catch (PatternSyntaxException e) {
			exc = e;
		}
		return (exc == null);
	}
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		mlc = new DefaultMessageListenerContainer();
		
		mlc.setConnectionFactory((CachingConnectionFactory)SpringApplicationContext.getBean("cachingConnectionFactory"));
		mlc.setDestination((ActiveMQTopic)SpringApplicationContext.getBean("processInitiatorTopic"));
		mlc.setMessageListener(this);
		mlc.setSessionTransacted(true);
		mlc.setConcurrentConsumers(1);
		mlc.setAutoStartup(true);
		mlc.initialize();
		mlc.start();
	}
	
	
	public String getRegex() {
		return regex;
	}
	public void setRegex(String regex) {
		this.regex = this.xtorRegex = regex;
	}



}