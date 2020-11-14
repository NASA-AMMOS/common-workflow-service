package jpl.cws.engine;

import jpl.cws.core.service.SpringApplicationContext;
import org.camunda.bpm.engine.ProcessEngine;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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
	}
	
	
	/**
	 * This method gets called when tomcat is starting up
	 */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("Worker detected that tomcat is coming up.");

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
