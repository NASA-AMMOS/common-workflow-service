package jpl.cws.engine.listener;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.code.CodeService;
import jpl.cws.core.log.CwsWorkerLoggerFactory;

/**
 * MessageListener that listens for code update messages to be published to a topic.
 * 
 * @author ghollins
 *
 */
public class CodeUpdateListener implements MessageListener, InitializingBean {
	
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private CodeService cwsCodeService;
	
	
	
	private Logger log;
	
	public CodeUpdateListener() { }
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("CodeUpdateListener constructor...");
	}
	
	
	@Override
	public void onMessage(final Message message) {
		if (message instanceof BytesMessage) {
			try {
				log.debug("----------------- GOT CODE UPDATE MESSAGE: "+message + " Redelivered?: "+ message.getJMSRedelivered());
				cwsCodeService.updateToLatestCode();
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