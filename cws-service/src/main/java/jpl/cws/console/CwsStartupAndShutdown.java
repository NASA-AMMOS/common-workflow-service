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

public class CwsStartupAndShutdown implements  ServletContextListener {

	public static boolean isShuttingDown = false;
	
	public CwsStartupAndShutdown() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		isShuttingDown = true;
		System.out.println("cws-ui web app has detected tomcat shutdown.");
		
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
		
		// Step 3: Force shutdown of any remaining Netty threads and MySQL cleanup
		try {
			System.out.println("  Forcing shutdown of Netty threads and MySQL cleanup...");
			
			// First, try to gracefully shutdown Netty event loop groups and thread pools
			EmbeddedActiveMQ activeMQServer = (EmbeddedActiveMQ)SpringApplicationContext.getBean("activeMQServer");
			if (activeMQServer != null && activeMQServer.getActiveMQServer() != null) {
				// Shutdown thread pools first to prevent IllegalMonitorStateException
				NettyShutdownUtil.shutdownActiveMQThreadPools(activeMQServer.getActiveMQServer());
				// Then shutdown Netty event loop groups
				NettyShutdownUtil.shutdownNettyEventLoopGroups(activeMQServer.getActiveMQServer());
			}
			
			// Shutdown MySQL connection cleanup threads
			NettyShutdownUtil.shutdownMysqlConnectionCleanup();
			
			// Use the utility class for comprehensive Netty cleanup
			NettyShutdownUtil.forceShutdownNettyThreads();
				
		} catch (Exception e) {
			System.out.println("  Error during Netty thread cleanup: " + e.getMessage());
			e.printStackTrace();
		}
		
		// Step 4: Shutdown ActiveMQ server last
		try {
			System.out.println("  Shutting down ActiveMQ server...");
			EmbeddedActiveMQ activeMQServer = (EmbeddedActiveMQ)SpringApplicationContext.getBean("activeMQServer");
			if (activeMQServer != null) {
				System.out.println("INFO: ActiveMQ server found, stopping...");
				activeMQServer.stop();
				System.out.println("  ActiveMQ server stopped successfully.");
			} else {
				System.out.println("INFO: ActiveMQ server bean is null during shutdown");
			}
		} catch (Exception e) {
			System.out.println("  Error stopping ActiveMQ server: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("cws-ui web app has detected tomcat startup.");
		
		// Register Netty shutdown hook as a safety net
		NettyShutdownUtil.registerShutdownHook();
		
		// Add focused logging for Artemis startup
		System.out.println("INFO: Starting CWS with Artemis debugging enabled...");
		
		try {
			System.out.println("DEBUG: Attempting to get ActiveMQ server bean...");
			EmbeddedActiveMQ activeMQServer = (EmbeddedActiveMQ)SpringApplicationContext.getBean("activeMQServer");
			if (activeMQServer != null) {
				System.out.println("DEBUG: ActiveMQ server bean found: " + activeMQServer);
			} else {
				System.out.println("ERROR: ActiveMQ server bean is null - startup may fail");
			}
		} catch (Exception e) {
			System.out.println("ERROR: Failed to get ActiveMQ server bean: " + e.getMessage());
			e.printStackTrace();
		}
		
		try {
			CwsEmailerService cwsEmailerService = (CwsEmailerService)SpringApplicationContext.getBean("cwsEmailerService");
			cwsEmailerService.sendNotificationEmails("CWS Startup", "CWS Starting up...");
		}
		catch (Throwable t) {
			System.out.println("ERROR: sending email on startup failed.");
			t.printStackTrace();
		}

	}

}
