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

/**
 * Service class for worker.
 * 
 * @author ghollins
 *
 */
public class WorkerStartupAndShutdown implements ServletContextListener {
	
	
	/**
	 * This method gets called when tomcat is shutting down.
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("Worker detected that tomcat is going down.");
		
		// Step 1: Shutdown message listener containers first
		System.out.println("  Shutting down DefaultMessageListenerContainers...");
		Map<String,DefaultMessageListenerContainer> beans = SpringApplicationContext.getBeansOfType(DefaultMessageListenerContainer.class);
		for (Entry<String,DefaultMessageListenerContainer> bean : beans.entrySet()) {
			DefaultMessageListenerContainer container = bean.getValue();
			System.out.println("    container.stop: " + container);
			if (container.isRunning()) {
				container.stop();
			}
			System.out.println("    container.shutdown: " + container);
			container.shutdown();
		}
		
		// Step 2: Properly close connection factory and underlying connections
		try {
			System.out.println("  Shutting down connection factory...");
			CachingConnectionFactory cc = (CachingConnectionFactory)SpringApplicationContext.getBean("cachingConnectionFactory");
			if (cc != null) {
				// Close all cached connections
				cc.resetConnection();
				cc.destroy();
				System.out.println("    CachingConnectionFactory destroyed successfully.");
			}
			
			// Also close the underlying ActiveMQ connection factory
			ActiveMQConnectionFactory acf = (ActiveMQConnectionFactory)SpringApplicationContext.getBean("connectionFactory");
			if (acf != null) {
				acf.close();
				System.out.println("    ActiveMQConnectionFactory closed successfully.");
			}
		} catch (Exception e) {
			System.out.println("  Error destroying connection factory: " + e.getMessage());
			e.printStackTrace();
		}
		
		// Step 3: Stop worker daemons manually
		try {
			System.out.println("  Stopping worker daemons...");
			WorkerHeartbeatDaemon workerHeartbeatDaemon = (WorkerHeartbeatDaemon)
					SpringApplicationContext.getBean("workerHeartbeatDaemon");
			WorkerDaemon workerDaemon = (WorkerDaemon)
					SpringApplicationContext.getBean("workerDaemon");
			WorkerExternalTaskLockDaemon workerExternalTaskLockDaemon = (WorkerExternalTaskLockDaemon)
					SpringApplicationContext.getBean("workerExternalTaskLockDaemon");
			WorkerService workerService = (WorkerService)
					SpringApplicationContext.getBean("workerService");
			
			if (workerHeartbeatDaemon != null) {
				System.out.println("    Stopping WorkerHeartbeatDaemon...");
				workerHeartbeatDaemon.stopDaemon();
				try {
					workerHeartbeatDaemon.join(5000); // wait up to 5s to terminate
					if (workerHeartbeatDaemon.isAlive()) {
						System.out.println("    WorkerHeartbeatDaemon did not stop gracefully within 5 seconds");
					} else {
						System.out.println("    WorkerHeartbeatDaemon stopped successfully");
					}
				} catch (InterruptedException e) {
					System.out.println("    Interrupted while waiting for WorkerHeartbeatDaemon to stop");
					Thread.currentThread().interrupt();
				}
			}
			
			if (workerDaemon != null) {
				System.out.println("    Interrupting WorkerDaemon...");
				workerDaemon.interrupt();
			}
			
			if (workerExternalTaskLockDaemon != null) {
				System.out.println("    Interrupting WorkerExternalTaskLockDaemon...");
				workerExternalTaskLockDaemon.interrupt();
			}
			
			if (workerService != null) {
				System.out.println("    Bringing worker down...");
				workerService.bringWorkerDown();
			}
			
			System.out.println("    Worker daemons stopped successfully.");
		} catch (Exception e) {
			System.out.println("  Error stopping worker daemons: " + e.getMessage());
			e.printStackTrace();
		}

		// Step 4: Force shutdown of any remaining Netty threads and MySQL cleanup
		try {
			System.out.println("  Forcing shutdown of Netty threads and MySQL cleanup...");
			
			// First, try to gracefully shutdown client-side Netty components
			ActiveMQConnectionFactory acf = (ActiveMQConnectionFactory)SpringApplicationContext.getBean("connectionFactory");
			if (acf != null) {
				NettyShutdownUtil.shutdownClientNettyComponents(acf);
			}
			
			// Shutdown MySQL connection cleanup threads
			NettyShutdownUtil.shutdownMysqlConnectionCleanup();
			
			// Use the utility class for comprehensive Netty cleanup
			NettyShutdownUtil.forceShutdownNettyThreads();
				
		} catch (Exception e) {
			System.out.println("  Error during Netty thread cleanup: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	/**
	 * This method gets called when tomcat is starting up
	 */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("Worker detected that tomcat is coming up.");
		
		// Register Netty shutdown hook as a safety net
		NettyShutdownUtil.registerShutdownHook();

		CwsEngineProcessApplication app = (CwsEngineProcessApplication)
				SpringApplicationContext.getBean("cwsEngineProcessApplication");
		System.out.println("CwsEngineProcessApplication = " + app);
		ProcessEngine pe = (ProcessEngine)
				SpringApplicationContext.getBean("processEngine2");
		System.out.println("ProcessEngine  = " + pe);

		//
		// Startup CwsEngineProcessApplication, now that tomcat is up and running
        //
		app.onDeploymentFinished(pe);
	}
}
