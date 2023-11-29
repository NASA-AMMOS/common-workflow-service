package jpl.cws.console;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.SQLException;

import jpl.cws.process.initiation.InitiatorsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import jpl.cws.core.code.CodeService;
import jpl.cws.core.db.DbService;
import jpl.cws.core.service.SecurityService;
import jpl.cws.service.CwsConsoleService;

/**
 * cws-ui webapp startup functionality
 * 
 * @author ghollins
 *
 */
public class CwsConsoleStartup implements InitializingBean, DisposableBean {
	private static final Logger log = LoggerFactory.getLogger(CwsConsoleStartup.class);
	
	@Autowired private DbService cwsDbService;
	@Autowired private CwsConsoleService cwsConsoleService;
	@Autowired private InitiatorsService cwsInitiatorsService;
	@Autowired private SecurityService cwsSecurityService;
	@Autowired private CodeService cwsCodeService;
	@Autowired private ProcessStatusDaemon processStatusDaemon;
	@Autowired private ElasticAndWorkerCleanupDaemon elasticAndWorkerCleanupDaemon;
	@Autowired private ExternalTaskDaemon externalTaskDaemon;
	@Autowired private WorkerMonitorBackgroundThread workerMonitorBackgroundThread;
	
	@Value("${cws.enable.cloud.autoscaling}") private String cwsEnableCloudAutoscaling;
	@Autowired private AwsMetricsPublisherBackgroundThread awsMetricsPublisherBackgroundThread;
	
	public CwsConsoleStartup() {
		log.trace("CwsConsoleStartup constructor...");
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		initCwsTables();
		
		transitionPendingRowsToDisabledIfAppropriate();
		
		deployStartupProcessDefinitions();
		
		startProcessStatusDaemon();
		
		startWorkerMonitorBackgroundThread();
		
		startElasticAndWorkerCleanupDaemon();

		startExternalTaskDaemon();
		
		// TODO: create config that launches a particular class name
		// so that a new provider can be seamlessly integrated without
		// changing this code.
		if (cwsEnableCloudAutoscaling.equals("true")) {
			startAwsMetricsPublisherBackgroundThread();
		}
		
		cwsSecurityService.populateInMemoryCwsTokenMapFromDb();
		
		// Populate DB with same as code above
		cwsCodeService.persistInProgressCode(cwsCodeService.getLatestCode());

		cwsInitiatorsService.loadInitiators();
	}	
	
	
	/**
	 * 
	 */
	private void initCwsTables() {
		try {
			cwsDbService.initCwsTables();
		} catch (SQLException e) {
			log.error("Problem while initializing CWS tables", e);
		}
	}
	
	
	/**
	 * 
	 */
	private void transitionPendingRowsToDisabledIfAppropriate() {
		String cwsKeepPendingRows = System.getenv("CWS_KEEP_PENDING_ROWS_ON_STARTUP");
		log.debug("transitionPendingRowsToDisabledIfAppropriate (cwsKeepPendingRows = " +
				cwsKeepPendingRows + ")...");
		
		if (cwsKeepPendingRows != null && cwsKeepPendingRows.equals("false")) {
			try {
				cwsDbService.changeSchedWorkerProcInstRowStatus("pending", "disabled", null);
			} catch (Exception e) {
				log.error("Problem while transitioning 'pending' rows to 'disabled' in DB", e);
			}
		}
	}
	
	
	/**
	 * 
	 */
	private void deployStartupProcessDefinitions() {
		String cwsHome = System.getenv("CWS_HOME");
		if (cwsHome == null || cwsHome.isEmpty()) {
			log.error("no CWS_HOME environmental variable set!!");
			return;
		}
		
		// Directory to look for process definition files (BPMN XML)
		//
		File directory = new File(cwsHome+"/bpmn");
		log.info("Deploying process definitions from '" +
			directory.getAbsolutePath() + "' directory...");
		
		// Setup a filename filter
		//
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept( File dir, String name ) {
				return name.matches(".*?.bpmn");
			}
		};
		
		// Scan file system for any process definitions,
		// and send message to queue requesting that they be deployed.
		//
		for(File bpmnFile : directory.listFiles(filter)) {
			log.info("DEPLOYING BPMN FILE: " + bpmnFile.getAbsolutePath());
			try {
				String errorMsg = cwsConsoleService.deployProcessDefinitionXmlFile(bpmnFile);
				
				if (errorMsg != null) {
					log.error("Problem creating/sending deployment message for BPMN file: " + bpmnFile.getAbsolutePath() + " -- " + errorMsg);
				}
			} catch (Exception e) {
				log.error("Problem deploying initial proc def file", e);
			}
		}
	}
	
	
	/**
	 * 
	 */
	private void startElasticAndWorkerCleanupDaemon() {
		elasticAndWorkerCleanupDaemon.start();
	}

	/**
	 *
	 */
	private void startExternalTaskDaemon() {
		externalTaskDaemon.start();
	}
	
	
	/**
	 * 
	 */
	private void startProcessStatusDaemon() {
		processStatusDaemon.start();
	}
	
	/**
	 * 
	 */
	private void startAwsMetricsPublisherBackgroundThread() {
		awsMetricsPublisherBackgroundThread.start();
	}
	
	
	/**
	 * 
	 */
	private void startWorkerMonitorBackgroundThread() {
		workerMonitorBackgroundThread.start();
	}
	
	
	@Override
	public void destroy() throws Exception {
		log.warn("Destroying bean...");
		log.warn("  Interrupting processStatusDaemon bean...");
		processStatusDaemon.interrupt();
		log.warn("  Interrupting elasticAndWorkerCleanupDaemon bean...");
		elasticAndWorkerCleanupDaemon.interrupt();
		log.warn("  Interrupting externalTaskDaemon bean...");
		externalTaskDaemon.interrupt();
		log.warn("  Interrupting awsMetricsPublisherBackgroundThread bean...");
		awsMetricsPublisherBackgroundThread.interrupt();
		log.warn("  Interrupting workerMonitorBackgroundThread bean...");
		workerMonitorBackgroundThread.interrupt();
		log.warn("Done with 'destroy' actions.");
	}
	
}
