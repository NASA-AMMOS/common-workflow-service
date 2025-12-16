package jpl.cws.console;

import java.util.Map;
import java.util.Map.Entry;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import jpl.cws.core.log.CwsEmailerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import jpl.cws.core.service.SpringApplicationContext;
import jpl.cws.core.util.NettyShutdownUtil;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CwsStartupAndShutdown implements  ServletContextListener {

	private static final Logger log = LoggerFactory.getLogger(CwsStartupAndShutdown.class);
	
	public static boolean isShuttingDown = false;
	
	public CwsStartupAndShutdown() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		isShuttingDown = true;
		log.info("cws-ui web app has detected tomcat shutdown.");
		
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
		
		// Step 3: Force shutdown of any remaining Netty threads and MySQL cleanup
		try {
			log.info("  Forcing shutdown of Netty threads and MySQL cleanup...");
			
			// First, try to gracefully shutdown Netty event loop groups and thread pools
			if (SpringApplicationContext.isContextAvailable()) {
				try {
					EmbeddedActiveMQ activeMQServer = (EmbeddedActiveMQ)SpringApplicationContext.getBean("activeMQServer");
					if (activeMQServer != null && activeMQServer.getActiveMQServer() != null) {
						// Shutdown thread pools first to prevent IllegalMonitorStateException
						NettyShutdownUtil.shutdownActiveMQThreadPools(activeMQServer.getActiveMQServer());
						// Then shutdown Netty event loop groups
						NettyShutdownUtil.shutdownNettyEventLoopGroups(activeMQServer.getActiveMQServer());
					}
				} catch (Exception e) {
					log.error("  Error accessing ActiveMQ server bean during Netty cleanup: " + e.getMessage(), e);
				}
			}
			
			// Shutdown MySQL connection cleanup threads
			NettyShutdownUtil.shutdownMysqlConnectionCleanup();
			
			// Use the utility class for comprehensive Netty cleanup
			NettyShutdownUtil.forceShutdownNettyThreads();
				
		} catch (Exception e) {
			log.error("  Error during Netty thread cleanup: " + e.getMessage(), e);
		}
		
		// Step 4: Shutdown ActiveMQ server last
		if (SpringApplicationContext.isContextAvailable()) {
			try {
				log.info("  Shutting down ActiveMQ server...");
				EmbeddedActiveMQ activeMQServer = (EmbeddedActiveMQ)SpringApplicationContext.getBean("activeMQServer");
				if (activeMQServer != null) {
					log.info("INFO: ActiveMQ server found, stopping...");
					activeMQServer.stop();
					log.info("  ActiveMQ server stopped successfully.");
				} else {
					log.info("INFO: ActiveMQ server bean is null during shutdown");
				}
			} catch (Exception e) {
				log.error("  Error stopping ActiveMQ server: " + e.getMessage(), e);
			}
		} else {
			log.warn("  Spring context not available, skipping ActiveMQ server shutdown");
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("cws-ui web app has detected tomcat startup.");
		
		// Register Netty shutdown hook as a safety net
		NettyShutdownUtil.registerShutdownHook();
		
		// Add focused logging for Artemis startup
		log.info("INFO: Starting CWS with Artemis debugging enabled...");
		
		try {
			log.debug("DEBUG: Attempting to get ActiveMQ server bean...");
			EmbeddedActiveMQ activeMQServer = (EmbeddedActiveMQ)SpringApplicationContext.getBean("activeMQServer");
			if (activeMQServer != null) {
				log.debug("DEBUG: ActiveMQ server bean found: " + activeMQServer);
			} else {
				log.error("ERROR: ActiveMQ server bean is null - startup may fail");
			}
		} catch (Exception e) {
			log.error("ERROR: Failed to get ActiveMQ server bean: " + e.getMessage(), e);
		}
		
		try {
			CwsEmailerService cwsEmailerService = (CwsEmailerService)SpringApplicationContext.getBean("cwsEmailerService");
			cwsEmailerService.sendNotificationEmails("CWS Startup", "CWS Starting up...");
		}
		catch (Throwable t) {
			log.error("ERROR: sending email on startup failed.", t);
		}

	}

}
