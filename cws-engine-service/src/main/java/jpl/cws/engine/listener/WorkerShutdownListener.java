package jpl.cws.engine.listener;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.log.CwsWorkerLoggerFactory;
import jpl.cws.core.service.SpringApplicationContext;

/**
 * MessageListener that listens for worker shutdown messages to be published to a topic.
 * 
 * @author jwood
 *
 */
public class WorkerShutdownListener implements MessageListener, InitializingBean {
	
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private SpringApplicationContext springApplicationContext;
	@Autowired private SchedulerDbService schedulerDbService;
	
	@Value("${cws.install.type}") private String cwsInstallType;
	
	private Logger log;
	
	public WorkerShutdownListener() { }
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("SystemShutdownListener constructor...");
	}
	
	private void doShutdown() {

		try {
			String cmdLineString = "../../../stop_cws.sh";
			
			CommandLine cmdLine = CommandLine.parse(cmdLineString);
			
			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
	
			ExecuteWatchdog watchdog = new ExecuteWatchdog(60 * 1000);
			
			Executor executor = new DefaultExecutor();
			executor.setWatchdog(watchdog);
			
			log.info("About to execute '" + cmdLineString + "'");
			executor.execute(cmdLine, resultHandler);
	
			// Wait for the process to complete
			//
			resultHandler.waitFor();
	
			// Get the exit value, log it, and put it in return map
			//
			int exitValue = resultHandler.getExitValue();
			log.info("Command '" + cmdLineString + "' exit value is " + exitValue);
		}
		catch (Exception e) {
			log.error("Exception doShutdown", e);
			throw new RuntimeException(e); // FIXME: do we really want to do this?
		}
	}
	
	@Override
	public void onMessage(final Message message) {
		if (message instanceof BytesMessage) {
			try {
				log.debug("----------------- GOT WORKER SHUTDOWN MESSAGE: "+message + " Redelivered?: "+ message.getJMSRedelivered());
				
				log.info("****************** WORKER SHUTDOWN **********  cwsInstallType = " + cwsInstallType);
				
				if (cwsInstallType.equals("worker_only")) {
					
					// Only shutdown worker_only type, let the ConsoleShutdownListener take care of the console shutdowns...
					doShutdown();
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
	
	
	//FIXME:  implement a cleanup (via Spring hook??) method that runs:
	//  executor.shutdown();

}