package jpl.cws.core.engine.plugins;


import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin registered in bpm-platform.xml
 * 
 * NOTE:  this is currently not used (commented out in bpm-platform.xml),
 *        but will serve as an example for future development (if needed).
 * 
 * @author ghollins
 *
 */
public class CustomIncidentHandlerPlugin implements ProcessEnginePlugin {
	private static final Logger log = LoggerFactory.getLogger(CustomIncidentHandlerPlugin.class);
	
	@Override
	public void preInit(
			ProcessEngineConfigurationImpl processEngineConfiguration) {
		log.info("&&&&&&&& preInit");
		
	}
	
	
	@Override
	public void postInit(
			ProcessEngineConfigurationImpl processEngineConfiguration) {
		log.info("&&&&&&&& postInit");
		
		//List<IncidentHandler> customIncidentHandlers = new ArrayList<IncidentHandler>();
		//
		//processEngineConfiguration.setCustomIncidentHandlers(customIncidentHandlers);
	}
	
	
	@Override
	public void postProcessEngineBuild(ProcessEngine processEngine) {
		log.info("&&&&&&&& postProcessEngineBuild");
	}

}
