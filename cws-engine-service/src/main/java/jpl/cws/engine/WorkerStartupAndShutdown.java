package jpl.cws.engine;

import java.util.Map;
import java.util.Map.Entry;

import jpl.cws.core.service.SpringApplicationContext;
import jpl.cws.core.util.NettyShutdownUtil;
import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for worker.
 * 
 * @author ghollins
 *
 */
public class WorkerStartupAndShutdown implements ServletContextListener {
	
	private static final Logger log = LoggerFactory.getLogger(WorkerStartupAndShutdown.class);
	
	/**
	 * This method gets called when tomcat is shutting down.
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("Worker detected that tomcat is going down.");
		
		// Step 1: Shutdown message listener containers first
		log.info("  Shutting down DefaultMessageListenerContainers...");
		if (SpringApplicationContext.isContextAvailable()) {
			try {
				Map<String,DefaultMessageListenerContainer> beans = SpringApplicationContext.getBeansOfType(DefaultMessageListenerContainer.class);
				for (Entry<String,DefaultMessageListenerContainer> bean : beans.entrySet()) {
					DefaultMessageListenerContainer container = bean.getValue();
					log.debug("    container.stop: " + container);
					if (container.isRunning()) {
						container.stop();
					}
					log.debug("    container.shutdown: " + container);
					container.shutdown();
				}
			} catch (Exception e) {
				log.error("  Error shutting down message listener containers: " + e.getMessage(), e);
			}
		} else {
			log.warn("  Spring context not available, skipping message listener container shutdown");
		}
		
		// Step 2: Properly close connection factory and underlying connections
		if (SpringApplicationContext.isContextAvailable()) {
			try {
				log.info("  Shutting down connection factory...");
				CachingConnectionFactory cc = (CachingConnectionFactory)SpringApplicationContext.getBean("cachingConnectionFactory");
				if (cc != null) {
					// Close all cached connections
					cc.resetConnection();
					cc.destroy();
					log.info("    CachingConnectionFactory destroyed successfully.");
				}
				
				// Also close the underlying ActiveMQ connection factory
				ActiveMQConnectionFactory acf = (ActiveMQConnectionFactory)SpringApplicationContext.getBean("connectionFactory");
				if (acf != null) {
					acf.close();
					log.info("    ActiveMQConnectionFactory closed successfully.");
				}
			} catch (Exception e) {
				log.error("  Error destroying connection factory: " + e.getMessage(), e);
			}
		} else {
			log.warn("  Spring context not available, skipping connection factory shutdown");
		}
		
		// Step 3: Stop worker daemons manually
		if (SpringApplicationContext.isContextAvailable()) {
			try {
				log.info("  Stopping worker daemons...");
				WorkerHeartbeatDaemon workerHeartbeatDaemon = (WorkerHeartbeatDaemon)
						SpringApplicationContext.getBean("workerHeartbeatDaemon");
				WorkerDaemon workerDaemon = (WorkerDaemon)
						SpringApplicationContext.getBean("workerDaemon");
				WorkerExternalTaskLockDaemon workerExternalTaskLockDaemon = (WorkerExternalTaskLockDaemon)
						SpringApplicationContext.getBean("workerExternalTaskLockDaemon");
				WorkerService workerService = (WorkerService)
						SpringApplicationContext.getBean("workerService");
			
				if (workerHeartbeatDaemon != null) {
					log.info("    Stopping WorkerHeartbeatDaemon...");
					workerHeartbeatDaemon.stopDaemon();
					try {
						workerHeartbeatDaemon.join(5000); // wait up to 5s to terminate
						if (workerHeartbeatDaemon.isAlive()) {
							log.warn("    WorkerHeartbeatDaemon did not stop gracefully within 5 seconds");
						} else {
							log.info("    WorkerHeartbeatDaemon stopped successfully");
						}
					} catch (InterruptedException e) {
						log.warn("    Interrupted while waiting for WorkerHeartbeatDaemon to stop");
						Thread.currentThread().interrupt();
					}
				}
				
				if (workerDaemon != null) {
					log.info("    Interrupting WorkerDaemon...");
					workerDaemon.interrupt();
				}
				
				if (workerExternalTaskLockDaemon != null) {
					log.info("    Interrupting WorkerExternalTaskLockDaemon...");
					workerExternalTaskLockDaemon.interrupt();
				}
				
				if (workerService != null) {
					log.info("    Bringing worker down...");
					workerService.bringWorkerDown();
				}
				
				log.info("    Worker daemons stopped successfully.");
			} catch (Exception e) {
				log.error("  Error stopping worker daemons: " + e.getMessage(), e);
			}
		} else {
			log.warn("  Spring context not available, skipping worker daemon shutdown");
		}

		// Step 4: Force shutdown of any remaining Netty threads and MySQL cleanup
		try {
			log.info("  Forcing shutdown of Netty threads and MySQL cleanup...");
			
			// First, try to gracefully shutdown client-side Netty components
			if (SpringApplicationContext.isContextAvailable()) {
				try {
					ActiveMQConnectionFactory acf = (ActiveMQConnectionFactory)SpringApplicationContext.getBean("connectionFactory");
					if (acf != null) {
						NettyShutdownUtil.shutdownClientNettyComponents(acf);
					}
				} catch (Exception e) {
					log.error("  Error accessing connection factory bean during Netty cleanup: " + e.getMessage(), e);
				}
			}
			
			// Shutdown MySQL connection cleanup threads
			NettyShutdownUtil.shutdownMysqlConnectionCleanup();
			
			// Use the utility class for comprehensive Netty cleanup
			NettyShutdownUtil.forceShutdownNettyThreads();
				
		} catch (Exception e) {
			log.error("  Error during Netty thread cleanup: " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * This method gets called when tomcat is starting up
	 */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("Worker detected that tomcat is coming up.");
		
		// Register Netty shutdown hook as a safety net
		NettyShutdownUtil.registerShutdownHook();

		CwsEngineProcessApplication app = (CwsEngineProcessApplication)
				SpringApplicationContext.getBean("cwsEngineProcessApplication");
		log.info("CwsEngineProcessApplication = " + app);
		ProcessEngine pe = (ProcessEngine)
				SpringApplicationContext.getBean("processEngine2");
		log.info("ProcessEngine  = " + pe);

		//
		// Startup CwsEngineProcessApplication, now that tomcat is up and running
        //
		app.onDeploymentFinished(pe);
	}
}
