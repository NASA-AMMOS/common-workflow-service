package jpl.cws.console;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import jpl.cws.core.log.CwsEmailerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import jpl.cws.core.service.SpringApplicationContext;

public class CwsStartupAndShutdown implements  ServletContextListener {

	public static boolean isShuttingDown = false;
	
	public CwsStartupAndShutdown() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		isShuttingDown = true;
		System.out.println("cws-ui web app has detected tomcat shutdown.");
		System.out.println("  Shutting down DefaultMessageListenerContainers...");
		Map<String,DefaultMessageListenerContainer> beans = SpringApplicationContext.getBeansOfType(DefaultMessageListenerContainer.class);
		for (Entry<String,DefaultMessageListenerContainer> bean : beans.entrySet()) {
			DefaultMessageListenerContainer container = bean.getValue();
			System.out.println(" A   container.stop: " + container);
			if (container.isRunning()) {
				container.stop();
			}
			System.out.println(" A   container.shutdown: " + container);
			container.shutdown();
		}
		
		CachingConnectionFactory cc = (CachingConnectionFactory)SpringApplicationContext.getBean("cachingConnectionFactory");
		cc.destroy();
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("cws-ui web app has detected tomcat startup.");
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
