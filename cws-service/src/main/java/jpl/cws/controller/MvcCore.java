package jpl.cws.controller;
import java.util.List;
import java.util.Set;

import jpl.cws.core.db.SchedulerDbService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import jpl.cws.core.web.DiskUsage;
import jpl.cws.scheduler.SchedulerQueueUtils;
import jpl.cws.service.CwsConsoleService;
import jpl.cws.service.camunda.CamundaExecutionService;

@Component
public class MvcCore {
	private static final Logger log = LoggerFactory.getLogger(MvcCore.class);

	@Autowired
	protected CamundaExecutionService cwsExecutionService;
	@Autowired
	protected SchedulerQueueUtils cwsSchedulerUtils;
	@Autowired
	private CwsConsoleService cwsConsoleService;

	@Value("${cws.console.app.root}")
	private String appRoot;
	@Value("${cws.version}")
	private String version;
	@Value("${cws.db.type}")
	private String dbType;
	@Value("${cws.db.host}")
	private String dbHost;
	@Value("${cws.db.name}")
	private String dbName;
	@Value("${cws.db.port}")
	private String dbPort;
	@Value("${cws.elasticsearch.protocol}")
	private String esProtocol;
	@Value("${cws.elasticsearch.hostname}")
	private String esHost;
	@Value("${cws.elasticsearch.index.prefix}")
	private String esIndexPrefix;
	@Value("${cws.elasticsearch.port}")
	private String esPort;
	@Value("${cws.auth.scheme}")
	private String authScheme;
	@Value("${cws.install.dir}")
	private String installDir;
	@Value("${cws.tomcat.lib}")
	private String tomcatLib;
	@Value("${cws.tomcat.bin}")
	private String tomcatBin;
	@Value("${cws.tomcat.home}")
	private String tomcatHome;
	@Value("${cws.tomcat.webapps}")
	private String tomcatWebapps;
	@Value("${cws.project.webapp.root}")
	private String projectWebappRoot;
	@Value("${cws.history.days.to.live}")
	private String historyDaysToLive;
	@Value("${cws.history.level}")
	private String historyLevel;

	public MvcCore() {
	}

	protected ModelAndView buildModel(String page, String message) {
		ModelAndView model = new ModelAndView(page);
		model.addObject("base", appRoot);
		model.addObject("msg", message);
		if (projectWebappRoot != null && !projectWebappRoot.isEmpty()) {
			model.addObject("cwsProjectWebappRoot", projectWebappRoot);
		}
		log.debug("MODEL: " + model.getModel());
		return model;
	}


	protected ModelAndView buildHomeModel(String message) {
		ModelAndView model = new ModelAndView("home");
		model.addObject("base", appRoot);
		model.addObject("msg", message);

		log.trace("MODEL for Home page: " + model.getModel());
		return model;
	}

	protected ModelAndView buildSummaryModel(String message) {
		ModelAndView model = new ModelAndView("summary");
		model.addObject("base", appRoot);
		model.addObject("msg", message);

		// Get stats for proc defs
		//
		List<ProcessDefinition> processDefs = cwsExecutionService.listProcessDefinitions();
		model.addObject("numTotalProcDefs", processDefs.size());
		int numActivePd = 0;
		for (ProcessDefinition pd : processDefs) {
			if (pd != null && !pd.isSuspended()) {
				numActivePd++;
			}
		}
		model.addObject("numActiveProcDefs", numActivePd);

		log.trace("MODEL for Summary page: " + model.getModel());
		return model;
	}


	protected ModelAndView buildDeploymentsModel(String message) {
		log.trace("buildDeploymentsModel...");
		ModelAndView model = new ModelAndView("deployments");
		try {
			model.addObject("base", appRoot);
			model.addObject("msg", message);

			// Add list of (the latest) process definitions to the model
			//
			model.addObject("procDefs", cwsExecutionService.listProcessDefinitions());

			log.trace("MODEL for Deployments page: " + model.getModel());
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}

	protected ModelAndView buildLogsModel(String message) {
		log.trace("buildLogsModel...");
		ModelAndView model = new ModelAndView("logs");
		try {
			model.addObject("base", appRoot);
			model.addObject("msg", message);

			// Add list of (the latest) process definitions to the model
			//
			model.addObject("procDefs", cwsExecutionService.listProcessDefinitions());
			model.addObject("workerIds", cwsConsoleService.getAllWorkerIds());

			log.trace("MODEL for Logs page: " + model.getModel());
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}

	protected ModelAndView buildHistoryModel(String message) {
		log.trace("buildHistoryModel...");
		ModelAndView model = new ModelAndView("history");
		try {
			model.addObject("base", appRoot);
			model.addObject("msg", message);

			log.trace("MODEL for History page: " + model.getModel());
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}

	/**
	 *
	 */
	protected ModelAndView buildProcessesModel(String message) {
		ModelAndView model = new ModelAndView("processes");
		try {
			model.addObject("base", appRoot);
			model.addObject("msg", message);

			// Add list of (the latest) process definitions to the model
			//
			model.addObject("procDefs", cwsExecutionService.listProcessDefinitions());

			log.trace("MODEL for Processes page: " + model.getModel());
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}


	/**
	 *
	 */
	protected ModelAndView buildConfigurationModel(String message) {
		ModelAndView model = new ModelAndView("configuration");
		try {
			model.addObject("base", appRoot);
			model.addObject("msg", message);

			model.addObject("version", version);
			model.addObject("dbType", dbType);
			model.addObject("dbHost", dbHost);
			model.addObject("dbName", dbName);
			model.addObject("dbPort", dbPort);
			model.addObject("esProtocol", esProtocol);
			model.addObject("esHost", esHost);
			model.addObject("esIndexPrefix", esIndexPrefix);
			model.addObject("esPort", esPort);
			model.addObject("authScheme", authScheme);
			model.addObject("installDir", installDir);
			model.addObject("tomcatLib", tomcatLib);
			model.addObject("tomcatBin", tomcatBin);
			model.addObject("tomcatHome", tomcatHome);
			model.addObject("tomcatWebapps", tomcatWebapps);
			model.addObject("historyDaysToLive", historyDaysToLive);
			model.addObject("historyLevel", historyLevel);
			model.addObject("javaHome", System.getenv("JAVA_HOME"));
			model.addObject("javaVersion", Runtime.version().toString());
			model.addObject("camundaVersion", System.getenv("CAMUNDA_VER"));

			DiskUsage diskUsage = cwsConsoleService.getDiskUsage();

			model.addObject("databaseSize", diskUsage.databaseSize);
			model.addObject("workersInfo", diskUsage.workers);

			log.trace("MODEL for Configuration page: " + model.getModel());
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}

	/**
	 *
	 */
	protected ModelAndView buildModelerModel() {
		ModelAndView model = new ModelAndView("modeler");
		try {
			model.addObject("base", appRoot);

			log.trace("MODEL for Modeler page: " + model.getModel());
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}


	/**
	 *
	 */
	protected ModelAndView buildDocumentationModel(String message) {
		ModelAndView model = new ModelAndView("documentation");
		try {
			model.addObject("base", appRoot);
			model.addObject("msg", message);
			model.addObject("cwsVersion", version);
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}


	/**
	 * Model for "Workers" web page
	 */
	protected ModelAndView buildWorkersModel() {
		ModelAndView model = new ModelAndView("workers");
		model.addObject("base", appRoot);
		model.addObject("msg", "");

		// Add list of process definition IDs to the model
		//
		List<ProcessDefinition> procDefs = cwsExecutionService.listProcessDefinitions();
		model.addObject("procDefs", procDefs);

		model.addObject("workers", cwsConsoleService.getWorkersUiDTO(procDefs));
		model.addObject("externalWorkers", cwsConsoleService.getExternalWorkersUiDTO());

		model.addObject("workersTitle", cwsConsoleService.getWorkersTitle());

		try {
			Set<org.apache.activemq.broker.Connection> clients = cwsSchedulerUtils.getAmqClients();
			model.addObject("amqClients", clients);
		} catch (Exception e) {
			log.error("There was a problem getting listing of AMQ clients", e);
		}

		return model;
	}

	protected ModelAndView buildModelerModel(String message) {
		log.trace("Building modeler's model...");
		ModelAndView model = new ModelAndView("modeler");
		try {
			model.addObject("base", appRoot);
			model.addObject("msg", message);

			log.trace("MODEL for Modeler page: " + model.getModel());
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}

	protected ModelAndView buildApiDocsModel(String message) {
		log.trace("Building apidocs's model...");
		ModelAndView model = new ModelAndView("api-docs");
		try {
			model.addObject("base", appRoot);
			model.addObject("msg", message);

			log.trace("MODEL for Modeler page: " + model.getModel());
		} catch (Throwable t) {
			log.error("Unexpected exception", t);
		}
		return model;
	}
}