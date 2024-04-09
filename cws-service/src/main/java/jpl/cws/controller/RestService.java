package jpl.cws.controller;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.db.SchedulerJob;
import jpl.cws.core.service.ProcessService;
import jpl.cws.core.web.DiskUsage;
import jpl.cws.core.web.JsonResponse;
import jpl.cws.core.web.JsonResponse.Status;
import jpl.cws.core.web.WebUtils;
import jpl.cws.core.web.WebUtils.RestCallResult;
import jpl.cws.process.initiation.CwsProcessInitiator;
import jpl.cws.process.initiation.InitiatorsService;
import jpl.cws.scheduler.CwsProcessInstance;
import jpl.cws.scheduler.LogHistory;
import jpl.cws.scheduler.Scheduler;
import jpl.cws.service.CwsConsoleService;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@RequestMapping("/api")
public class RestService extends MvcCore {
	private static final Logger log = LoggerFactory.getLogger(RestService.class);

	@Autowired private RepositoryService repositoryService;
	@Autowired private ProcessService processService;
	@Autowired private SchedulerDbService schedulerDbService;
	@Autowired private Scheduler scheduler;
	@Autowired private CwsConsoleService cwsConsoleService;
	@Autowired private ExternalTaskService externalTaskService;
	@Autowired private SchedulerDbService dbService;
	@Autowired private ManagementService managementService;
	@Autowired private InitiatorsService cwsInitiatorsService;
	
	@Autowired
	@Qualifier("jmsProcessInitiatorTemplate")
	private JmsTemplate jmsProcessInitiatorTemplate;
	
	@Value("${cws.console.app.root}") private String appRoot;
	@Value("${cws.install.hostname}") private String hostName;

	@Value("${cws.elasticsearch.protocol}") private String elasticsearchProtocolName;
	@Value("${cws.elasticsearch.hostname}") private String elasticsearchHostname;
	@Value("${cws.elasticsearch.port}") private String elasticsearchPort;

	@Value("${cws.elasticsearch.use.auth}") private String elasticsearchUseAuth;
	@Value("${cws.elasticsearch.username}") private String elasticsearchUsername;
	@Value("${cws.elasticsearch.password}") private String elasticsearchPassword;

	public RestService() {}
	
	
	/**
	 * Gets the contents of the initiators XML context file
	 * 
	 */
	@ApiOperation(value="Gets the contents of the initiators XML context file.", tags = {"Initiators"}, produces = "application/xml")
	@RequestMapping(value="/initiators/getXmlContextFile", method=GET)
	public @ResponseBody String getXmlContextFile() {
		try {
			return cwsInitiatorsService.getCurXmlContextFile();
		}
		catch (Exception e) {
			log.error("Unexpected exception while getting initiators context XML contents", e);
			return "error";
		}
	}
	
	
	/**
	 * Refreshes initiators from XML file
	 * 
	 */
	@ApiOperation(value = "Refreshes initiators from a new XML file.", tags = {"Initiators"}, consumes = "application/xml", produces = "text/plain")
	@ApiImplicitParams(
			@ApiImplicitParam(name = "newXmlContext", value = "New XML context to update initiators with.", required = true, paramType = "query")
	)
	@RequestMapping(value="/initiators/updateInitiatorsContextXml", method=POST)
	public @ResponseBody String refreshInitiatorsFromXml(HttpServletResponse response,
			@RequestParam("newXmlContext") String newXmlContext) {
		
		try {
			cwsInitiatorsService.updateAndRefreshInitiators(newXmlContext);
		} catch (Exception e) {
			log.error("Problem while refreshInitiatorsFromXml()", e);
			return "ERROR MESSAGE: " + e.getMessage();
		}
		
		return "success";
	}
	
	
	/**
	 * Refreshes initiators from current working initiators XML file
	 * 
	 */
	@ApiOperation(value = "Refreshes initiators from current working initiators XML file.", tags = {"Initiators"}, produces = "text/plain")
	@RequestMapping(value="/initiators/loadInitiatorsContextXml", method=POST)
	public @ResponseBody String refreshInitiatorsFromXml(HttpServletResponse response) {
		
		try {
			String xmlContents = cwsInitiatorsService.getCurXmlContextFile();
			cwsInitiatorsService.updateAndRefreshInitiators(xmlContents);
		} catch (Exception e) {
			log.error("Problem while loadInitiatorsContextXml()", e);
			return "ERROR MESSAGE: " + e.getMessage();
		}
		
		return "success";
	}

	/**
	 * TODO: Switch over to this after UI changes allow editing of a single initiator
	 *
	 * Adds or updates single initiator
	 */
	@ApiOperation(value = "Updates a single initiator.", tags = {"Initiators"}, consumes = "application/xml", produces = "text/plain")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "newXmlContext", value = "New XML context to update initiators with.", required = true, paramType = "query"),
			@ApiImplicitParam(name = "beanName", value = "Bean name of the initiator to update.", required = true, paramType = "query")
	})
	@RequestMapping(value="/initiators/updateSingleInitiator", method=POST)
	public @ResponseBody String updateSingleInitiatorFromXml(
			@RequestParam("newXmlContext") String newXmlContext,
			@RequestParam("beanName") String beanName) {

		try {
			cwsInitiatorsService.updateSingleInitiator(newXmlContext, beanName);
		} catch (Exception e) {
			log.error("Problem while updateSingleInitiator()", e);
			return "ERROR MESSAGE: " + e.getMessage();
		}

		return "success";
	}

	/**
	 * TODO: Make this unnecessary with UI changes
	 *
	 * Updates only changed or new initiators
	 */
	@ApiOperation(value = "Updates only changed or new initiators.", tags = {"Initiators"}, consumes = "application/xml", produces = "text/plain")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "newXmlContext", value = "New XML context to update initiators with.", required = true, paramType = "query")
	})
	@RequestMapping(value="/initiators/updateChangedInitiators", method=POST)
	public @ResponseBody String updateChangedInitiatorsFromXml(
			@RequestParam("newXmlContext") String newXmlContext) {

		try {
			cwsInitiatorsService.updateChangedInitiators(newXmlContext);
		} catch (Exception e) {
			log.error("Problem while updateChangedInitiatorsFromXml()", e);
			return "ERROR MESSAGE: " + e.getMessage();
		}

		return "success";
	}
	
	/**
	 * Updates a process initiator's enabled flag.
	 * 
	 */
	@ApiOperation(value = "Updates a process initiator's enabled flag.", tags = {"Initiators"}, produces = "text/plain")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "initiatorId", value = "ID of the initiator to update.", required = true, paramType = "path"),
			@ApiImplicitParam(name = "enabled", value = "New enabled status of the initiator.", required = true, paramType = "query")
	})
	@RequestMapping(value="/initiators/{initiatorId}/enabled", method=POST)
	public @ResponseBody ModelAndView setInitiatorEnabled(
		@PathVariable String initiatorId,
		@RequestParam("enabled") boolean enabled) {
		
		try {
			if (enabled) {
				cwsInitiatorsService.enableAndStartInitiator(initiatorId);
			}
			else {
				cwsInitiatorsService.disableAndStopInitiator(initiatorId);
			}
		}
		catch (Exception e) {
			log.error("A problem occured when setting enabled status to " +enabled, e);
			return buildModel("initiators", e.getMessage());
		}
		
		// Success!
		return buildModel("login", "updated initiator enabled to " + enabled);
	}

	@ApiOperation(value = "Enables / disables all process initiators.", tags = {"Initiators"}, produces = "text/plain")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "enabled", value = "New enabled status of the initiators.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/initiators/all/enabled", method = POST)
	public @ResponseBody ModelAndView setAllInitiatorsEnabled(
			@RequestParam("enabled") boolean enabled) {

		try {
			if (enabled) {
				cwsInitiatorsService.enableAndStartAllInitiators();
			} else {
				cwsInitiatorsService.disableAndStopAllInitiators();
			}
		} catch (Exception e) {
			log.error("A problem occured when setting enabled status to " + enabled, e);
			return buildModel("initiators", e.getMessage());
		}
		return buildModel("login", "updated initiator enabled to " + enabled);
	}
	
	
	/**
	 * Gets a process initiator's enabled flag.
	 * 
	 */
	@ApiOperation(value = "Gets a process initiator's enabled flag.", tags = {"Initiators"}, produces = "text/plain")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "initiatorId", value = "ID of the initiator to get.", required = true, paramType = "path")
	})
	@RequestMapping(value="/initiators/{initiatorId}/enabled", method=GET)
	public @ResponseBody String isInitiatorEnabled(
			@PathVariable String initiatorId) {
		try {
			log.trace("REST::isInitiatorEnabled isInitiatorEnabled + " + initiatorId);
			CwsProcessInitiator initiator = 
					cwsConsoleService.getProcessInitiatorById(initiatorId);
			if (initiator == null) {
				return "ERROR: Could not find an initiator with ID '" + initiatorId + "'";
			}
			log.trace("REST::isInitiatorEnabled::returning " + initiator.isEnabled());
			return initiator.isEnabled() ? "true" : "false";
		}
		catch (Exception e) {
			log.error("isInitiatorEnabled exception", e);
		}
		return "error";
	}

	/**
	 * Gets all process initiators enabled flag.
	 *
	 */
	@ApiOperation(value = "Gets all process initiators enabled flag.", tags = {"Initiators"}, produces = "application/json")
	@RequestMapping(value = "initiators/all/enabled", method = GET)
	public @ResponseBody Map<String, String> areAllInitiatorsEnabled () {
		try {
			log.trace("REST::areAllInitiatorsEnabled");
			List<CwsProcessInitiator> initiators = cwsConsoleService.getAllProcessInitiators();
			Map<String, String> statusMap = new HashMap<>();
			for (int i = 0; i < initiators.size(); i++) {
				String status = "";
				if (initiators.get(i).isEnabled()) {
					status = "true";
				} else {
					status = "false";
				}
				statusMap.put(initiators.get(i).getInitiatorId(), status);
			}
			return statusMap;
		} catch (Exception e) {
			log.error("areAllInitiatorsEnabled exception", e);
		}
		return null;
	}
	
	
	/**
	 * Returns ModelAndView table body representing the current set of Initiators.
	 * 
	 */
	@ApiOperation(value = "Returns ModelAndView table body representing the current set of Initiators.", tags = {"Initiators"}, produces = "text/html")
	@RequestMapping(value = "/initiators/getInitiatorsHtmlTable", method = GET)
	public ModelAndView getInitiatorsHtmlTable() {
		ModelAndView mav = new ModelAndView("initiators-table");
		mav.addObject("base", appRoot);
		try {
			Map<String,CwsProcessInitiator> initiatorsMap = cwsInitiatorsService.getProcessInitiators();
			List<CwsProcessInitiator> initiators = new ArrayList<CwsProcessInitiator>();
			for (Entry<String,CwsProcessInitiator> entry : initiatorsMap.entrySet()) {
				initiators.add(entry.getValue());
			}
			log.trace("REST CALL (getInitiatorsHtmlTable) returning table with " + initiators.size() + " initiators...");
			mav.addObject("initiators", initiators);
		} catch (Exception e) {
			log.error("There was a problem getting listing of initiators", e);
		}
		return mav;
	}
	
	
	/**
	 * Notify confused User to use POST instead of GET
	 * 
	 */
	@ApiOperation(hidden = true, value = "Notify confused User to use POST instead of GET", tags = {"Initiators"}, produces = "text/plain")
	@RequestMapping(value="/deployments/deployProcessDefinition", method = GET)
	public @ResponseBody String provideDeployProcessDefinitionInfo() {
		return "You can upload a file by POSTing to this same URL.";
	}
	
	
	/**
	 * Deploys a new process definition from a filename (for deployment from the modeler)
	 * 
	 */
	@ApiOperation(value = "Deploys a new process definition from a filename (for deployment from the modeler).", tags = {"Deployments"}, produces = "text/plain")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "filename", value = "Name of the file to deploy.", required = true, paramType = "query"),
		@ApiImplicitParam(name = "xmlData", value = "XML data to deploy.", required = true, paramType = "query")
	})
	@RequestMapping(value="/deployments/deployModelerFile", method = POST)
	public @ResponseBody String deployModelerFile(
			@RequestParam("filename") String filename,
			@RequestParam("xmlData") String xmlData) {

		// Don't allow filename to contain path modifiers
		if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
			return "ERROR: Input filename '" + filename + "' cannot contain any path modifiers";
		}

		BufferedWriter bufferedWriter = null;
		File f = null;
		try {
			String tDir = System.getProperty("java.io.tmpdir");
			String fullFilename = tDir + File.separator + filename + ".bpmn";
			f = new File(fullFilename);
        	Writer writer = new FileWriter(f);
        	bufferedWriter = new BufferedWriter(writer);
        	bufferedWriter.write(xmlData);
		} catch (IOException e1) {
			e1.printStackTrace();
			return "ERROR: " + e1.getMessage();
		} finally {
            try {
                if (bufferedWriter != null) {
                	bufferedWriter.close();
                }
            } catch(Exception e2) {
                 e2.printStackTrace();
            }
        }

		MultipartFile multipartFile = null;
		try (FileInputStream input = new FileInputStream(f)) {
			multipartFile = new MockMultipartFile("f", f.getName(), "text/plain", IOUtils.toByteArray(input));
			return doDeployProcessDefinition(multipartFile);
		} catch (IOException e4) {
			e4.printStackTrace();
			return "ERROR: " + e4.getMessage();
		}
		finally {
			f.delete();
		}
	}
	
	
	/**
	 * Deploys a new process definition via a UI-uploaded file
	 * @throws IOException 
	 * 
	 */
	@ApiOperation(value = "Deploys a new process definition via a UI-uploaded file.", tags = {"Deployments"}, produces = "text/plain")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "file", value = "File to deploy.", required = true, paramType = "query")
	})
	@RequestMapping(value="/deployments/deployProcessDefinition", method = POST)
	public @ResponseBody ModelAndView deployProcessDefinition(
			@RequestParam("file") MultipartFile file) {
		return buildDeploymentsModel(doDeployProcessDefinition(file));
	}
	
	/**
	 * General purpose deployment logic
	 *
	 */
	private String doDeployProcessDefinition(MultipartFile file) {
		String origFileName = file.getOriginalFilename();
		
		try {
			// TODO: validate more things about the uploaded file such as extension
			if (!file.isEmpty()) {
				// Convert MultipartFile to a File
				File bpmnFile = new File(cwsConsoleService.getCwsHome() + "/bpmn/" + origFileName);
				try {
					bpmnFile.createNewFile();
					FileOutputStream fos = new FileOutputStream(bpmnFile);
					fos.write(file.getBytes());
					fos.close();
				} catch (IOException e1) {
					return "ERROR: Failed to deploy due to internal error: " + e1.getMessage();
				}
				
				String errorMsg = cwsConsoleService.deployProcessDefinitionXmlFile(bpmnFile);
				
				try { Thread.sleep(500); } catch (InterruptedException e) { ; }
				if (errorMsg == null) {
					return "Deployed process definition: "+origFileName + 
						".<br/><br/><small>Newly deployed file can be found at: " + cwsConsoleService.getCwsHome() + "/bpmn/" + origFileName + "</small>";
				}
				else {
					return errorMsg;
				}
				
			} else {
				return "ERROR: You failed to upload '" + origFileName + "' because the file was empty.";
			}	
		} catch (Exception e) {
			log.error("Unexpected error: ", e);
			return e.getMessage();
		}
	}


	/**
	 * Constructs Elasticsearch URL
	 *
	 * @param subPath The subPath for the elasticsearch query, e.g., /_delete_by_query
	 * @return fully constructed elasticsearch URL string
	 */
	private String constructElasticsearchUrl(String subPath) {
		String urlString = elasticsearchProtocolName + "://" + elasticsearchHostname + ":" + elasticsearchPort + subPath;
		return urlString;
	}

	/**
	 *
	 * @return boolean indicating whether elasticsearch requires authentication
	 */
	private Boolean elasticsearchUseAuth() {
		return elasticsearchUseAuth.equalsIgnoreCase("Y");
	}
	
	
	/**
	 * Undeploys a process definition.
	 *
	 */
	@ApiOperation(value = "Undeploys a process definition.", tags = {"Deployments"}, produces = "text/plain")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "deploymentId", value = "ID of the deployment to undeploy.", required = true, paramType = "path")
	})
	@RequestMapping(value = "/processes/processDefinition/{processDefKey}/undeploy", method = GET, produces="application/json")
	public @ResponseBody String unDeployProcessDefinition(
			@PathVariable String processDefKey) {
		try {
			
			if (!processService.isProcDefKeyDeployed(processDefKey)) {
				throw new Exception("Not found");
			}
			if (processService.isProcDefKeyRunning(processDefKey)) {
				throw new Exception("Running");
			}
			if (processService.isProcDefKeyAcceptingNew(processDefKey)) {
				throw new Exception("Accepting new");
			}
			
			log.info("Undeploying process definition '"+processDefKey+"'...");
			List<ProcessDefinition> procDefs = repositoryService.createProcessDefinitionQuery().list();
			for (ProcessDefinition procDef : procDefs) {
				String procDefKey = procDef.getKey();
				if (procDefKey.equals(processDefKey)) {

					String deploymentId = procDef.getDeploymentId();

					log.info("About to unregister process application for deployment ID: "+deploymentId+" ("+procDefKey+")...");
					managementService.unregisterProcessApplication(deploymentId, true);
					log.info("Unregistered process application for deployment ID: "+deploymentId);
					
					log.info("About to delete deployment ID: "+deploymentId+" ("+procDefKey+")...");
					repositoryService.deleteDeployment(deploymentId, true);
					log.info("Deleted deployment ID: "+deploymentId);
					
					// Clear process definition from all CWS tables
					//
					schedulerDbService.deleteProcessDefinition(procDefKey);
					
					// Send single to all workers to re-evaluate their process definition list and in-memory data structures
					cwsConsoleService.sendWorkerConfigChangeTopicMessage();
				}
			}
		}
		catch (Exception e) {

			String message = "A problem occurred while trying to undeploy procDefKey: " + processDefKey + " (" + e.getMessage() + ")";
			log.error(message);
			
			return new JsonResponse(JsonResponse.Status.FAIL, message).toString();
		}
		
		return new JsonResponse(JsonResponse.Status.SUCCESS, "Undeployed procDefKey '" + processDefKey + "'").toString();
	}

	@ApiOperation(value = "Schedules a process definition", tags = {"Deployments"}, produces = "text/plain")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "processDefKey", value = "Key of the process definition to schedule.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "processBusinessKey", value = "Business key of the process to schedule.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "initiationKey", value = "Initiation key of the process to schedule.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "processPriority", value = "Priority of the process to schedule.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "processVariables", value = "Variables of the process to schedule.", required = false, paramType = "query")
	
	})
	@RequestMapping(value = "/process/{processDefKey}/schedule", method = POST)
	public @ResponseBody String scheduleProcess(
			@ApiIgnore final HttpSession session,
			@PathVariable String processDefKey,
			@RequestParam (value = "processBusinessKey", required=false) String processBusinessKey,
			@RequestParam (value = "initiationKey", required=false) String initiationKey,
			@RequestParam (value = "processPriority", required=false, defaultValue="default") String processPriority,
			@RequestParam MultiValueMap<String,String> processVariables
			) {
		
		log.info("******* REST (POST) SCHEDULING Process '" + processDefKey + "' " +
				"with (processVariables="+processVariables+", " +
				"processBusinessKey=" + processBusinessKey +", " +
				"initiationKey=" + initiationKey + ", " +
				"processPriority=" + processPriority + ")...");
		
		// Get the process priority to use
		//
		int priority = 0;
		if (processPriority.equals("default")) {
			priority = Scheduler.DEFAULT_PROCESS_PRIORITY;
		}
		else {
			try {
				priority = Integer.parseInt(processPriority);
			}
			catch (Exception e) {
				return "ERROR: invalid processPriority specified ('" + processPriority + "')";
			}
		}
		
		// Get validated process variables map
		// (or return error message, if there was a problem)
		//
		Map<String,String> procVariablesMap = null;
		try {
			procVariablesMap = scheduler.validateScheduleRequest(processDefKey, processVariables);
		}
		catch (Exception e) {
			return e.getMessage();
		}
		
		// Schedule the process
		SchedulerJob schedulerJob = null;
		try {
			schedulerJob = scheduler.scheduleProcess(processDefKey, procVariablesMap, processBusinessKey, initiationKey, priority);
		} catch (Exception e) {
			log.error("FAILED TO SCHEDULE PROCESS: "+processDefKey);
			Timestamp tsNow = new Timestamp(DateTime.now().getMillis());
			schedulerJob = new SchedulerJob(null, tsNow, tsNow, null, null, processDefKey,
					priority, procVariablesMap, processBusinessKey, initiationKey, "failedToSchedule", e.getMessage());
			return new GsonBuilder().setPrettyPrinting().create().toJson(schedulerJob);
		}
		
		return new GsonBuilder().setPrettyPrinting().create().toJson(schedulerJob);
	}
	
	/**
	 * REST method used to get status information about a process instance
	 * 
	 */
	@ApiOperation(value = "Gets status information about a process instance.", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "uuid", value = "UUID of the process instance to get status for.", required = true, paramType = "path")
	})
	@RequestMapping(value = "/process-instance/{uuid}/status", method = GET, produces="application/json")
	public @ResponseBody String getProcessInstanceStatus(
			@PathVariable String uuid,
			@ApiIgnore final HttpSession session) {
		
		log.debug("REST: getProcessInstanceStatus(" + uuid + ")");
		
		//
		// TODO: implement detection of history level, and warn user appropriately if
		//       they don't have enough information to get these results.
		//
		
		return cwsConsoleService.getProcInstStatusJson(uuid);
	}

    /**
     * Returns status counts for (proc_def_key, business_key) pair
     */
	@ApiOperation(value = "Gets status counts for (proc_def_key, business_key) pair.", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "businessKey", value = "Business key of the process instance to get status for.", required = true, paramType = "query"),
		@ApiImplicitParam(name = "procDefKey", value = "Process definition key of the process instance to get status for.", required = true, paramType = "query")
	})
    @RequestMapping(value="/stats/statsByBusinessKey", method = GET)
    public @ResponseBody Map<String,Integer> statsByBusinessKey(
            @RequestParam(value = "businessKey", required=true) String businessKey,
            @RequestParam(value = "procDefKey", required=true) String procDefKey
    ) {
        Map<String, Integer> ret = new HashMap<>();
        try {

            ret = cwsConsoleService.getStatsByBusinessKey(procDefKey, businessKey);
        }
        catch (Exception e) {
            log.error("Issue getting process instance stats", e);
        }
        return ret;
    }
	
	
	/**
	 * Returns latest successfully compiled code snippet from DB
	 */
	@ApiOperation(value = "Gets latest successfully compiled code snippet from DB.", tags = {"Snippets"}, produces = "text/plain")
	@RequestMapping(value="/snippets/getLatestCodeSnippet", method = GET)
	public @ResponseBody String getLatestCodeSnippet() {
		return cwsConsoleService.getLatestCode();
	}
	
	
	/**
	 * Returns latest code snippet from DB
	 */
	@ApiOperation(value = "Gets latest code snippet from DB.", tags = {"Snippets"}, produces = "text/plain")
	@RequestMapping(value="/snippets/getLatestInProgressCodeSnippet", method = GET)
	public @ResponseBody String getLatestInProgressCodeSnippet() {
		return cwsConsoleService.getLatestInProgressCode();
	}
	
	
	/**
	 * Saves UI-edited code to the database.
	 */
	@ApiOperation(value = "Saves UI-edited code snippet to the database.", tags = {"Snippets"}, produces = "text/plain")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "code", value = "Code to save.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/snippets/validateAndSaveSnippets", method = POST)
	public ModelAndView validateAndSaveSnippets(
			@RequestParam String code,
			@ApiIgnore final HttpSession session) {
		log.debug("REST: validateAndSaveSnippets");
		log.trace("REST: validateAndSaveSnippets, code=" + code);
		
		ModelAndView model = new ModelAndView("snippets");
		model.addObject("base", appRoot);
		
		String errors = cwsConsoleService.validateAndPersistCode(code);
		if (errors != null) {
			model.addObject("msg", "ERROR: invalid code.  Did not save to database.<br />" + errors);
			return model;
		}
		
		model.addObject("msg", "Saved the snippets");
		return model;
	}
	
	
	/**
	 * Sends a message to shutdown the entire system, including all remote workers
	 */
	@ApiOperation(value = "Sends a message to shutdown the entire system, including all remote workers.", tags = {"System"}, produces = "text/plain")
	@RequestMapping(value="/system/shutdown", method = GET)
	public @ResponseBody String doSystemShutdown() {
		return cwsConsoleService.doSystemShutdown();
	}


	/**
	 * REST method used to get logs
	 *
	 */
	@ApiOperation(value = "Gets logs using a scroll ID to keep track of already fetched data. Used on logs page.", tags = {"Logs"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "scrollId", value = "Scroll ID to keep track of already fetched data.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/logs/get/scroll", method = POST, produces="application/json")
	public @ResponseBody String getLogsScroll(
			@RequestParam(value = "scrollId") String scrollId) {
		String urlString = constructElasticsearchUrl("/_search/scroll");
		String jsonData = "{ \"scroll\" : \"1m\", \"scroll_id\" : \"" + scrollId + "\" }";

		log.trace("REST getLogs query = " + urlString);
		log.trace("REST getLogs jsonData = " + jsonData);

		try {
			RestCallResult restCallResult;
			if (elasticsearchUseAuth()) {
				// Authenticated call
				restCallResult = WebUtils.restCall(urlString, "POST", jsonData, null, null, "application/json; charset=utf-8", elasticsearchUsername, elasticsearchPassword);
			} else {
				// Unauthenticated call
				restCallResult = WebUtils.restCall(urlString, "POST", jsonData, null, null, "application/json; charset=utf-8");
			}
			if (restCallResult.getResponseCode() != 200) {
				return "ERROR";
			}
			return restCallResult.getResponse();
		} catch (Exception e) {
			log.error("Problem performing REST call to get log data", e);
		}

		return "ERROR";
	}

	/**
	 * REST method used to get the total number of log rows
	 *
	 */
	@ApiOperation(value = "Gets the total number of log rows.", tags = {"Logs"}, produces = "application/json")
	@RequestMapping(value="/logs/get/count", method = GET, produces="application/json")
	public @ResponseBody String getNumLogs() {
		String urlString = constructElasticsearchUrl("/_count");

		log.trace("REST getNumLogs query = " + urlString);

		try {
			RestCallResult restCallResult;
			if (elasticsearchUseAuth()) {
				// Authenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, "application/json; charset=utf-8", elasticsearchUsername, elasticsearchPassword);
			} else {
				// Unauthenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, "application/json; charset=utf-8");
			}
			if (restCallResult.getResponseCode() != 200) {
				return "ERROR";
			}
			return restCallResult.getResponse();
		} catch (Exception e) {
			log.error("Problem performing REST call to get count of log data (URL=" + urlString + ")", e);
		}

		return "ERROR";
	}

	/**
	 * REST method used to get logs on the logs page (shorter scroll timer)
	 *
	 */
	@ApiOperation(value = "Gets logs on the logs page (shorter scroll timer).", tags = {"Logs"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "source", value = "Source of the logs to get.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/logs/get/noScroll", method = GET, produces="application/json")
	public @ResponseBody String getLogsNoScroll(
			@RequestParam(value = "source") String source) {
		String urlString = constructElasticsearchUrl("/_search");

		log.debug("REST logs/get/noScroll query = " + urlString);

		try {
			String result = source;

			log.debug("logs/get/noScroll: result: " + result);
			RestCallResult restCallResult;
			if (elasticsearchUseAuth()) {
				// Authenticated call
				restCallResult = WebUtils.restCall(urlString, "POST", result, null, null, "application/json; charset=utf-8", elasticsearchUsername, elasticsearchPassword);
			} else {
				// Unauthenticated call
				restCallResult = WebUtils.restCall(urlString, "POST", result, null, null, "application/json; charset=utf-8");
			}
			if (restCallResult.getResponseCode() != 200) {
				log.error("Error with /logs/get/noScroll: " + restCallResult.getResponse() + "; " + restCallResult.getResponseMessage());
				return "ERROR: " + restCallResult.getResponse() + "; " + restCallResult.getResponseMessage();
			}
			return restCallResult.getResponse();
		} catch (Exception e) {
			log.error("Problem performing REST call to get log data (URL=" + urlString + ")", e);
		}

		return "ERROR";
	}

	/**
	 * REST method used to get logs
	 * 
	 */
	@ApiOperation(value = "Gets logs.", tags = {"Logs"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "source", value = "Source of the logs to get.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/logs/get", method = GET, produces="application/json")
	public @ResponseBody String getLogs(
			@RequestParam(value = "source") String source) {
		String urlString = constructElasticsearchUrl("/_search?scroll=5m&source=" + source + "&source_content_type=application/json");
		
		log.trace("REST getLogs query = " + urlString);
		
		try {
			RestCallResult restCallResult;
			if (elasticsearchUseAuth()) {
				// Authenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, "application/json; charset=utf-8", elasticsearchUsername, elasticsearchPassword);
			} else {
				// Unauthenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, "application/json; charset=utf-8");
			}
			if (restCallResult.getResponseCode() != 200) {
				return "ERROR";
			}
			return restCallResult.getResponse();
		} catch (Exception e) {
			log.error("Problem performing REST call to get log data (URL=" + urlString + ")", e);
		}
		
		return "ERROR";
	}
	
	
	/**
	 * REST method used to delete logs by procDefKey
	 * 
	 */
	@ApiOperation(value = "Deletes logs by procDefKey.", tags = {"Logs"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "procDefKey", value = "Process definition key to delete logs for.", required = true, paramType = "path")
	})
	@RequestMapping(value = "/logs/delete/{procDefKey}", method = DELETE, produces="application/json")
	public @ResponseBody String deleteLogsByProcDefKey(
			HttpServletResponse response,
			@PathVariable String procDefKey
			) {
		String urlString = constructElasticsearchUrl("/*/_delete_by_query");
		log.debug("REST deleteLogsByProcDefKey url = " + urlString);
		
		String data = "{ \"query\": { \"bool\": { \"must\": [ { \"match\": { \"procDefKey\": \"" + procDefKey + "\" } } ] } } }";
		log.debug("REST deleteLogsByProcDefKey data = " + data);
		
		try {
			RestCallResult restCallResult;
			if (elasticsearchUseAuth()) {
				// Authenticated call
				restCallResult = WebUtils.restCall(urlString, "POST", data, null, null, "application/json", elasticsearchUsername, elasticsearchPassword);
			} else {
				// Unauthenticated call
				restCallResult = WebUtils.restCall(urlString, "POST", data, null, null, "application/json");
			}
			
			if (restCallResult.getResponseCode() != 200) {
				
				throw new Exception(restCallResult.getResponse());
			}
			
			String strResponse = "{\"status\": \"SUCCESS\"}";

			return strResponse;
		}
		catch (Exception e) {
			
			String message = "A problem occurred while trying to delete log data (url=" + urlString + ", data=" + data + ", error=" + e.getMessage() + ")";
			log.error(message, e);

			String strResponse = "{\"status\": \"ERROR\"}";

			return strResponse;
		}
	}
	
	
	
	public class GsonUTCDateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

		private final DateFormat dateFormat;

		public GsonUTCDateAdapter() {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		@Override public synchronized JsonElement serialize(Date date,Type type,JsonSerializationContext jsonSerializationContext) {
			return new JsonPrimitive(dateFormat.format(date));
		}

		@Override public synchronized Date deserialize(JsonElement jsonElement,Type type,JsonDeserializationContext jsonDeserializationContext) {
			try {
				return dateFormat.parse(jsonElement.getAsString());
			} catch (ParseException e) {
				throw new JsonParseException(e);
			}
		}
	}
	
	
	/**
	 * REST method used to get history (logs + historical data)
	 * 
	 */
	@ApiOperation(value = "Gets history (logs + historical data).", tags = {"History"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "procInstId", value = "Process instance ID to get history for.", required = true, paramType = "path")
	})
	@RequestMapping(value = "/history/{procInstId}", method = GET, produces="application/json")
	public @ResponseBody String getHistory(@PathVariable String procInstId) {

		LogHistory history = cwsConsoleService.getHistoryForProcess(procInstId);

		Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonUTCDateAdapter()).create();

		return gson.toJson(history);
	}


	/**
	 * REST method used to get Elasticsearch stats
	 * 
	 */
	@ApiOperation(value = "Gets Elasticsearch stats.", tags = {"Elasticsearch"}, produces = "application/json")
	@RequestMapping(value = "/stats/es/indices", method = GET, produces="application/json")
	public @ResponseBody String getElasticsearchIndices() {
		String urlString = constructElasticsearchUrl("/_cat/indices?v&bytes=b&s=index&format=json");
		
		log.trace("REST query = " + urlString);
		
		try {
			RestCallResult restCallResult;
			if (elasticsearchUseAuth()) {
				// Authenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, null, elasticsearchUsername, elasticsearchPassword);
			} else {
				// Unauthenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, null);
			}
			if (restCallResult.getResponseCode() != 200) {
				return "ERROR";
			}
			return restCallResult.getResponse();
		} catch (Exception e) {
			log.error("Problem performing REST call to get ES stats (URL=" + urlString + ")", e);
		}
		
		return "ERROR";
	}
	

	/**
	 * REST method used to get Elasticsearch stats
	 * 
	 */
	@ApiOperation(value = "Gets Elasticsearch cluster health.", tags = {"Elasticsearch"}, produces = "application/json")
	@RequestMapping(value = "/stats/es/cluster/health", method = GET, produces="application/json")
	public @ResponseBody String getElasticsearchClusterHealth() {
		String urlString = constructElasticsearchUrl("/_cluster/health");
		
		log.trace("REST query = " + urlString);
		
		try {
			RestCallResult restCallResult;
			if (elasticsearchUseAuth()) {
				// Authenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, null, elasticsearchUsername, elasticsearchPassword);
			} else {
				// Unauthenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, null);
			}
			if (restCallResult.getResponseCode() != 200) {
				return "ERROR";
			}
			return restCallResult.getResponse();
		} catch (Exception e) {
			log.error("Problem performing REST call to get ES stats (URL=" + urlString + ")", e);
		}
		
		return "ERROR";
	}
	
	
	/**
	 * REST method used to get Elasticsearch stats
	 * 
	 */
	@ApiOperation(value = "Gets Elasticsearch stats.", tags = {"Elasticsearch"}, produces = "application/json")
	@RequestMapping(value = "/stats/es", method = GET, produces="application/json")
	public @ResponseBody String getElasticsearchStats() {
		String urlString = constructElasticsearchUrl("/_nodes/stats/_all");
		
		log.trace("REST query = " + urlString);
		
		try {
			RestCallResult restCallResult;
			if (elasticsearchUseAuth()) {
				// Authenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null, null, null, null, elasticsearchUsername, elasticsearchPassword);
			} else {
				// Unauthenticated call
				restCallResult = WebUtils.restCall(urlString, "GET", null);
			}
			if (restCallResult.getResponseCode() != 200) {
				return "ERROR";
			}
			return restCallResult.getResponse();
		} catch (Exception e) {
			log.error("Problem performing REST call to get ES stats (URL=" + urlString + ")", e);
		}
		
		return "ERROR";
	}
	
	
	/**
	 * Returns latest system stats (Db size, ES size, Disk space, Log sizes, etc...
	 */
	@ApiOperation(value = "Gets system stats.", tags = {"System"}, produces = "application/json")
	@RequestMapping(value="/stats/diskUsage", method = GET, produces = "application/json")
	public @ResponseBody String getDiskStats(HttpServletResponse response) {

		String errorMsg = "";
		
		try {
			DiskUsage diskUsage = cwsConsoleService.getDiskUsage();

			response.setStatus(HttpServletResponse.SC_OK);
			
			return diskUsage.toString();
			
		} catch (Exception e) {

			errorMsg = e.getMessage();
		}

		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		
		return new JsonResponse(Status.FAIL, errorMsg).toString();
	}


	/**
	 * Returns latest code snippet from DB
	 */
	@ApiOperation(value = "Gets latest code snippet from DB.", tags = {"Snippets"}, produces = "text/plain")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "snippetId", value = "ID of the snippet to get.", required = false, paramType = "query")
	})
	@RequestMapping(value="/stats/processInstanceStats", method = GET)
	public @ResponseBody Map<String,String> getProcessInstanceStats(
			@RequestParam(value = "lastNumHours", required=false) String lastNumHours
			) {
		
		return cwsConsoleService.getProcessInstanceStats(lastNumHours);
	}
	
	
	/*
	* Return JSON key values of process status
	* e.g. {PD1: {errors:4, pending:3,... },...}
	*/
	@ApiOperation(value = "Gets process instance stats (JSON).", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "lastNumHours", value = "Number of hours to get stats for.", required = false, paramType = "query")
	})
	@RequestMapping(value="/stats/processInstanceStatsJSON", method = GET)
	public @ResponseBody Map<String,Map<String,String>> getProcessInstanceStatsJSON(
			@RequestParam(value = "lastNumHours", required=false) String lastNumHours
			) {
		Map<String,Map<String,String>> ret = new HashMap<String,Map<String,String>>();
		try {	

			ret = cwsConsoleService.getProcessInstanceStatsJSON(lastNumHours);
		}
		catch (Exception e) {
			log.error("Issue getting process instance stats", e);
		}
		return ret;
	}
	
	
	/*
	*
	*
	*/
	@ApiOperation(value = "Gets pending process instances (JSON).", tags = {"Processes"}, produces = "application/json")
	@RequestMapping(value="/stats/pendingProcessesJSON", method = GET, produces="application/json")
	public @ResponseBody String getPendingProcessesJSON(HttpServletResponse response) {
		JsonArray json = new JsonArray();
		
		try {
			json = cwsConsoleService.getPendingProcessesJSON();
		}
		catch (Exception e) {
			log.error("Issue getting pending processes", e);

			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			
			return new JsonResponse(JsonResponse.Status.FAIL, e.getMessage()).toString();
		}

		response.setStatus(HttpServletResponse.SC_OK);
		
		return json.toString();
	}
	
	
	/**
	 * 
	 * FIXME: This can result in double-counting (e.g. a running task has an external task as well)
	 */
	@ApiOperation(value = "Gets number of running processes for each worker.", tags = {"Workers"}, produces = "application/json")
	@RequestMapping(value="/stats/workerNumRunningProcs", method = GET)
	public @ResponseBody Map<String,String> getWorkerNumRunningProcs() {
		
		Map<String, String> procs = cwsConsoleService.getWorkerNumRunningProcs();

		for (String workerId : procs.keySet()) {
			int count = Integer.parseInt(procs.get(workerId));
			
			// Get number of external tasks locked for this worker
			int numLocked = (int)externalTaskService.createExternalTaskQuery().workerId(workerId).locked().count();
			
			// For now just add them together
			int total = count + numLocked;

			procs.put(workerId, Integer.toString(total));
		}

		return procs;
	}
	
	
	/**
	 * 
	 * FIXME:  remove processVariables parameter below -- I don't think it's used
	 */
	@ApiOperation(value = "Update the number of process definitions a worker can be working on at any given time.", tags = {"Workers"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "workerId", value = "ID of the worker to update.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "procDefKey", value = "Key of the process definition to update.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "newLimit", value = "New limit for the worker.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "processVariables", value = "Process variables to update.", required = false, paramType = "query")
	})
	@RequestMapping(value = "/worker/{workerId}/{procDefKey}/updateWorkerProcDefLimit/{newLimit}", method = POST)
	public @ResponseBody String updateWorkerProcDefLimit(
			@ApiIgnore final HttpSession session,
			@PathVariable String workerId,
			@PathVariable String procDefKey,
			@PathVariable String newLimit,
			@RequestParam MultiValueMap<String,String> processVariables) {
		
		log.info("*** REST CALL ***  updateWorkerProcDefLimit (workerId='"+workerId+"', procDefKey='"+procDefKey+"', newLimit='"+newLimit+"')...");
		
		try {
			dbService.updateWorkerProcDefLimit(workerId, procDefKey, Integer.parseInt(newLimit));
		} catch (NumberFormatException e) {
			log.error("Specified invalid number via UI: " + newLimit);
			return "fail";
		} catch (Exception e) {
			log.error("Unexpected error", e);
			return "fail";
		}
		
		cwsConsoleService.sendWorkerConfigChangeTopicMessage();
		
		return "success";
	}


	/**
	 * Inserts or updates worker tag with name and value
	 *
	 */
	@ApiOperation(value = "Inserts or updates worker tag with name and value.", tags = {"Workers"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "workerId", value = "ID of the worker to update.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "name", value = "Name of the tag to update.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "value", value = "Value of the tag to update.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/worker/{workerId}/updateTag/{name}", method = POST, produces="application/json")
	public @ResponseBody String updateWorkerTag(
			HttpServletResponse response,
			@PathVariable String workerId,
			@PathVariable String name,
			@RequestParam(value = "value") String value) {

		try {
			dbService.updateWorkerTag(workerId, name, value);
		} catch (Exception e) {
			log.error("Unexpected error", e);

			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

			return new JsonResponse(JsonResponse.Status.FAIL, e.getMessage()).toString();
		}

		response.setStatus(HttpServletResponse.SC_OK);

		return new JsonResponse(JsonResponse.Status.SUCCESS, "Updated worker '" + workerId + "' tag: " + name + "='" + value + "'").toString();
	}


	/**
	 * Checks if procDefKey is deployed (exists)
	 * 
	 */
	@ApiOperation(value = "Checks if process definition key is deployed.", tags = {"Processes"}, produces = "text/plain")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "procDefKey", value = "Key of the process definition to check.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/isProcDefKeyDeployed", method = POST)
	public @ResponseBody String isProcDefKeyDeployed(
			@RequestParam(value = "procDefKey", required=true) String procDefKey) {
		
		log.trace("isProcDefKeyDeployed... (procDefKey="+procDefKey+")");

		boolean isValid = processService.isProcDefKeyDeployed(procDefKey);
		
		log.trace("/isProcDefKeyDeployed returning " + isValid);

		return isValid+"";
	}
	
	
	/**
	* Get list of all workers with active status for the process
	*/
	@ApiOperation(value = "Gets list of all workers with active status for the process.", tags = {"Workers", "Processes"}, produces = "application/json")
	@RequestMapping(value="/worker/{procDefKey}/getWorkersForProc", method = GET)
	public @ResponseBody String getWorkersForProc(@PathVariable String procDefKey) {

		List<Map<String,Object>> procWorkers = dbService.getWorkersForProcDefKey(procDefKey);
		
		return new GsonBuilder().setPrettyPrinting().create().toJson(procWorkers);
	}
	

	/**
	* Add new external worker
	*/
	@ApiOperation(value = "Adds new external worker.", tags = {"Workers"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "hostname", value = "Hostname of the worker to add.", required = true, paramType = "query")
	})
	@RequestMapping(value="/externalWorker/add", method = GET)
	public @ResponseBody String addExternalWorker(
			@RequestParam(value = "hostname") String hostname) {

		String workerId = UUID.randomUUID().toString();
		String workerName = dbService.createExternalWorkerRow(workerId, hostname);
		
		Map<String, Object> output = new HashMap<String, Object>();
		
		output.put("workerId",  workerId);
		output.put("workerName", workerName);

		return new GsonBuilder().setPrettyPrinting().create().toJson(output);
	}
	
	/**
	* Update external worker heartbeat
	*/
	@ApiOperation(value = "Updates external worker heartbeat.", tags = {"Workers"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "workerId", value = "ID of the worker to update.", required = true, paramType = "path")
	})
	@RequestMapping(value="/externalWorker/{workerId}/heartbeat", method = GET)
	public @ResponseBody void externalWorkerHeartbeat(@PathVariable String workerId) {

		dbService.updateExternalWorkerHeartbeat(workerId);
	}
	
	@ApiOperation(value = "Updates external worker.", tags = {"Workers"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "workerId", value = "ID of the worker to update.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "activeTopics", value = "Active topics of the worker to update.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "currentTopic", value = "Current topic of the worker to update.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "currentCommand", value = "Current command of the worker to update.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "currentWorkingDir", value = "Current working directory of the worker to update.", required = false, paramType = "query")
	})
	@RequestMapping(value = "/externalWorker/{workerId}/update", method = POST)
	public @ResponseBody String updateExternalWorker(
			@PathVariable String workerId,
			@RequestParam(value = "activeTopics", required=false) String activeTopics,
			@RequestParam(value = "currentTopic", required=false) String currentTopic,
			@RequestParam(value = "currentCommand", required=false) String currentCommand,
			@RequestParam(value = "currentWorkingDir", required=false) String currentWorkingDir) {
		
		try {
			if (activeTopics != null) {
				dbService.updateExternalWorkerActiveTopics(workerId, activeTopics);
			}
			
			if (currentTopic != null) {
				dbService.updateExternalWorkerCurrentTopic(workerId, currentTopic);
			}
			
			if (currentCommand != null) {
				dbService.updateExternalWorkerCurrentCommand(workerId, currentCommand);
			}
			
			if (currentWorkingDir != null) {
				dbService.updateExternalWorkerCurrentWorkingDir(workerId, currentWorkingDir);
			}
		} catch (Exception e) {
			log.error("Unexpected error", e);
			return "fail";
		}
		
		return "success";
	}
	
	/**
	 * 
	 * 
	 */
	@ApiOperation(value = "Gets the size of an instance", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "superProcInstId", value = "Super process instance ID to get size for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "procInstId", value = "Process instance ID to get size for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "procDefKey", value = "Process definition key to get size for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "status", value = "Status to get size for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "minDate", value = "Minimum date to get size for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "maxDate", value = "Maximum date to get size for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "maxReturn", value = "Maximum number of results to return.", required = false, paramType = "query")
	})
	@RequestMapping(value = "/processes/getInstancesSize", method = GET, produces="application/json")
	public @ResponseBody int getInstancesSize(
			@RequestParam(value = "superProcInstId", required=false) String superProcInstId,
			@RequestParam(value = "procInstId", required=false) String procInstId,
			@RequestParam(value = "procDefKey", required=false) String procDefKey,
			@RequestParam(value = "status", required=false) String status,
			@RequestParam(value = "minDate", required=false) String minDate,
			@RequestParam(value = "maxDate", required=false) String maxDate,
			@RequestParam(value = "maxReturn", required=false, defaultValue="5000") String maxReturn
			) {

		Integer intMaxReturn = Integer.parseInt(maxReturn);

		log.debug("REST:  getProcessInstancesSize (superProcInstId='" + superProcInstId +
				"', procInstId='" + procInstId +
				"', procDefKey='"+procDefKey+
				"', status='"+status+"', minDate="+minDate+", maxDate="+maxDate);
		
		int size = 0;
		try {
			size = dbService.getFilteredProcessInstancesSize(
					superProcInstId, procInstId, procDefKey, status, minDate, maxDate);
			if (intMaxReturn > 0 && intMaxReturn < size) {
				size = intMaxReturn;
			}
		}
		catch (Exception e) {
			log.error("Problem while getFilteredProcessInstancesSize", e);
		}
		return size;
	}

	@ApiOperation(value = "Gets the status of a process isntance ID", tags = {"Processes", "History"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "procInstId", value = "Process instance ID to get status for.", required = true, paramType = "path")
	})
	@RequestMapping(value="/history/getStatus/{procInstId}", method = GET)
	public @ResponseBody String getStatusByProcInstId(
			@PathVariable String procInstId) {
		List<CwsProcessInstance> instances = null;
		instances = cwsConsoleService.getFilteredProcessInstancesCamunda(
				null, procInstId, null, null, null, null, "DESC", 0);
		if (instances.size() == 0) {
			return null;
		} else {
			return instances.get(0).getStatus();
		}
	}
	
	/**
	 * REST method used to get Processes table JSON
	 * 
	 */
	@ApiOperation(value = "Gets camunda instances.", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "superProcInstId", value = "Super process instance ID to get instances for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "procInstId", value = "Process instance ID to get instances for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "procDefKey", value = "Process definition key to get instances for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "status", value = "Status to get instances for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "minDate", value = "Minimum date to get instances for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "maxDate", value = "Maximum date to get instances for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "dateOrderBy", value = "Date order by to get instances for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "page", value = "Page to get instances for.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "maxReturn", value = "Maximum number of results to return.", required = false, paramType = "query")
	})
	@RequestMapping(value = "/processes/getInstancesCamunda", method = GET, produces="application/json")
	public @ResponseBody String getProcessInstancesCamunda(
			@RequestParam(value = "superProcInstId",  required=false) String superProcInstId,
			@RequestParam(value = "procInstId",  required=false) String procInstId,
			@RequestParam(value = "procDefKey",  required=false) String procDefKey,
			@RequestParam(value = "status",      required=false) String status,
			@RequestParam(value = "minDate",     required=false) String minDate,
			@RequestParam(value = "maxDate",     required=false) String maxDate,
			@RequestParam(value = "dateOrderBy", required=false, defaultValue="DESC") String dateOrderBy,
			@RequestParam(value = "page", required=false, defaultValue="0") String page,
			@RequestParam(value = "maxReturn", required=false, defaultValue="5000") String maxReturn
			) {
		
		List<CwsProcessInstance> instances = null;
		try {

			Integer pageNum = Integer.parseInt(page);
			Integer intMaxReturn = Integer.parseInt(maxReturn);

			dateOrderBy = dateOrderBy.toUpperCase();
			if (!dateOrderBy.equals("DESC") && !dateOrderBy.equals("ASC")) {
				log.error("Invalid dateOrderBy of " + dateOrderBy + "!  Forcing to be 'DESC'");
				dateOrderBy = "DESC";
			}
			
			log.debug("REST:  getProcessInstances (superProcInstId='" + superProcInstId +
					"', procInstId='" + procInstId +
					"', procDefKey='"+procDefKey+
					"', status='"+status+"', minDate="+minDate+", maxDate="+maxDate+
					", dateOrderBy="+dateOrderBy+")");

			instances = cwsConsoleService.getFilteredProcessInstancesCamunda(
					superProcInstId, procInstId, procDefKey, status, minDate, maxDate, dateOrderBy, pageNum);

			if ((intMaxReturn != -1) && (instances.size() > intMaxReturn)) {
				instances = instances.subList(0, intMaxReturn);
			}

		}
		catch (Exception e) {
			log.error("Problem getting process instance information!", e);
			// return an empty set
			return new GsonBuilder().setPrettyPrinting().create().toJson(new ArrayList<CwsProcessInstance>());
		}
		return new GsonBuilder().serializeNulls().create().toJson(instances);
	}
	
	
	/**
	* List of all process definitions and number of workers selected for each
	*/
	@ApiOperation(value = "Gets process definitions and number of workers selected for each.", tags = {"Processes"}, produces = "application/json")
	@RequestMapping(value="/processes/getProcDefWorkerCount", method = GET)
	public @ResponseBody String getProcDefWorkerCount() {
		
		List<Map<String,Object>> procWorkers = dbService.getProcDefWorkerCount();
		
		return new GsonBuilder().setPrettyPrinting().create().toJson(procWorkers);
	}
	
	
	/**
	 * 
	 * 
	 */
	@ApiOperation(value = "Makes disabled processes pending", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "uuids", value = "UUIDs of the processes to make pending.", required = true, paramType = "body")
	})
	@RequestMapping(value = "/processes/makeDisabledRowsPending", method = POST)
	public @ResponseBody String makeDisabledRowsPending(
			@ApiIgnore final HttpSession session,
			@RequestBody List<String> uuids) {
		
		log.info("*** REST CALL ***  /processes/makeDisabledRowsPending ... " + uuids.size());
		
		int numRowsTransitioned = 0;
		try {
			numRowsTransitioned = 
				dbService.changeSchedWorkerProcInstRowStatus("disabled", "pending", uuids);
		} catch (Exception e) {
			log.error("Problem while transitioning 'disabled' rows to 'pending' in DB", e);
		}
		
		log.debug("*** REST CALL *** returning " + numRowsTransitioned);
		return "{ \"status\" : \"success\", \"message\" : \"Updated " + numRowsTransitioned + " rows.\"}";
	}
	
	
	/**
	 * 
	 * 
	 */
	@ApiOperation(value = "Makes pending processes disabled", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "uuids", value = "UUIDs of the processes to make disabled.", required = true, paramType = "body")
	})
	@RequestMapping(value = "/processes/makePendingRowsDisabled", method = POST)
	public @ResponseBody String makePendingRowsDisabled(
			@ApiIgnore final HttpSession session,
			@RequestBody List<String> uuids) {
		
		log.info("*** REST CALL ***  /processes/makePendingRowsDisabled ... " + uuids.size());
		
		int numRowsTransitioned = 0;
		try {
			numRowsTransitioned = 
				dbService.changeSchedWorkerProcInstRowStatus("pending", "disabled", uuids);
		} catch (Exception e) {
			log.error("Problem while transitioning 'pending' rows to 'disabled' in DB", e);
		}
		log.debug("*** REST CALL *** returning " + numRowsTransitioned);
		return "{ \"status\" : \"success\", \"message\" : \"Updated " + numRowsTransitioned + " rows.\"}";
	}


	/**
	 * Retry incidents
	 *
	 */
	@ApiOperation(value = "Retries processes that have the status 'Incident'.", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "retries", value = "Number of retries to set for the incidents.", required = true, paramType = "query"),
		@ApiImplicitParam(name = "uuids", value = "UUIDs of the processes to retry.", required = true, paramType = "body")
	})
	@RequestMapping(value = "/processes/retryIncidentRows", method = POST)
	public @ResponseBody ResponseEntity<String> retryIncidentRows(
			@ApiIgnore final HttpSession session,
			@RequestParam(defaultValue = "1") String retries,
			@RequestBody List<String> uuids) {

		log.info("*** REST CALL ***  /processes/retryIncidentRows ... " + uuids.size());

		int retries_;
		try {
			retries_ = Integer.parseInt(retries);
		} catch (NumberFormatException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Retries must be a number");
		}

		if (retries_ < 1) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Retries must be greater than or equal to 1");
		}

		int numRowsUpdated = 0;
		try {
			numRowsUpdated = schedulerDbService.setRetriesForUuids(uuids, retries_);
		} catch (Exception e) {
			log.error("Problem while retrying 'incident' rows in DB", e);
		}
		log.debug("*** REST CALL *** returning " + numRowsUpdated);
		return ResponseEntity.ok("{ \"status\" : \"success\", \"message\" : \"Updated " + numRowsUpdated + " rows.\"}");
	}

	/**
	 * Retry failedToStart
	 *
	 */
	@ApiOperation(value = "Retries processes that have the status 'Failed to Start'.", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "uuids", value = "UUIDs of the processes to retry.", required = true, paramType = "body")
	})
	@RequestMapping(value = "/processes/retryFailedToStart", method = POST)
	public @ResponseBody ResponseEntity<String> retryFailedToStart(
			@ApiIgnore final HttpSession session,
			@RequestBody List<String> uuids) {

		log.info("*** REST CALL ***  /processes/retryFailedToStart ... " + uuids.size());

		int numRowsUpdated = 0;
		try {
			numRowsUpdated = schedulerDbService.retryFailedToStart(uuids);
		} catch (Exception e) {
			log.error("Problem while retrying 'failedToStart' rows in DB", e);
		}
		log.debug("*** REST CALL *** returning " + numRowsUpdated);
		return ResponseEntity.ok("{ \"status\" : \"success\", \"message\" : \"Updated " + numRowsUpdated + " rows.\"}");
	}

	/**
	 * Mark 'fail' as resolved
	 *
	 */
	@ApiOperation(value = "Marks processes that have the status 'Fail' as resolved.", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "uuids", value = "UUIDs of the processes to mark as resolved.", required = true, paramType = "body")
	})
	@RequestMapping(value = "/processes/markResolved", method = POST)
	public @ResponseBody ResponseEntity<String> markResolved(
			@ApiIgnore final HttpSession session,
			@RequestBody List<String> procInstIds) {

		log.info("*** REST CALL ***  /processes/markResolved ... " + procInstIds.size());

		int numRowsUpdated = 0;
		try {
			numRowsUpdated = schedulerDbService.makeResolvedBulk(procInstIds);
		} catch (Exception e) {
			log.error("Problem while marking failed rows as resolved in DB", e);
		}
		log.debug("*** REST CALL *** returning " + numRowsUpdated);
		return ResponseEntity.ok("{ \"status\" : \"success\", \"message\" : \"Updated " + numRowsUpdated + " rows.\"}");
	}
	
	/**
	 * 
	 * 
	 */
	@ApiOperation(value = "Updates the enabled/disabled status of a process definition on a worker", tags = {"Processes", "Workers"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "workerId", value = "ID of the worker to update.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "procDefKey", value = "Key of the process definition to update.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "enabledFlag", value = "Flag to set the process definition to.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "processVariables", value = "Process variables to update.", required = false, paramType = "query")
	})
	@RequestMapping(value = "/worker/{workerId}/{procDefKey}/updateWorkerProcDefEnabled/{enabledFlag}", method = POST)
	public @ResponseBody String updateWorkerProcDefEnabled(
			@ApiIgnore final HttpSession session,
			@PathVariable String workerId,
			@PathVariable String procDefKey,
			@PathVariable String enabledFlag,
			@RequestParam MultiValueMap<String,String> processVariables) {
		
		log.info("*** REST CALL ***  updateWorkerProcDefEnabled (workerId='"+workerId+"', procDefKey='"+procDefKey+"', enabledFlag='"+enabledFlag+"')...");
		
		try {
			dbService.updateWorkerProcDefEnabled(workerId, procDefKey,
					processService.getDeploymentIdForProcDef(procDefKey),
					Boolean.parseBoolean(enabledFlag));
		} catch (Exception e) {
			log.error("Unexpected error", e);
			return "fail";
		}

		cwsConsoleService.sendWorkerConfigChangeTopicMessage();
		
		return "success";
	}

	/**
	 * Suspends a process definition given its procDefId
	 *
	 */
	@ApiOperation(value = "Suspends a process definition given its procDefId.", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "procDefId", value = "ID of the process definition to suspend.", required = true, paramType = "path")
	})
	@RequestMapping(value = "/deployments/suspend/{procDefId}", method = POST)
	public @ResponseBody String suspendProcDefId(
			@PathVariable String procDefId) {
		log.info("*** REST CALL *** suspendProcDefId (procDefId=" + procDefId + ")");
		String result = cwsConsoleService.suspendProcDefId(procDefId);
		return result;
	}

	/**
	 * Activates a suspended process definition given its procDefId
	 *
	 */
	@ApiOperation(value = "Activates a suspended process definition given its procDefId.", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "procDefId", value = "ID of the process definition to activate.", required = true, paramType = "path")
	})
	@RequestMapping(value = "/deployments/activate/{procDefId}", method = POST)
	public @ResponseBody String activateProcDefId(
			@PathVariable String procDefId ) {
		log.info ("*** REST CALL *** activateProcDefId (procDefId" + procDefId + ")");
		String result = cwsConsoleService.activateProcDefId(procDefId);
		return result;
	}

	/**
	 * Method that deletes a running process instance.
	 *
	 * Accepts an array of procInstIds and expects all of them to be running.
	 */
	@ApiOperation(value = "Deletes running process instances (Only pass running instances into this endpoint)", tags = {"Processes"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "procInstIds", value = "IDs of the process instances to delete. Expects all of the process instances in the list to be running.", required = true, paramType = "body")
	})
	@RequestMapping(value = "/processes/delete", method = POST)
	public @ResponseBody String deleteRunningProcInsts(
			@ApiIgnore final HttpSession session,
			@RequestBody List<String> procInstIds) {
		log.debug("*** REST CALL *** deleteRunningProcInsts");
		String result = cwsConsoleService.deleteRunningProcInst(procInstIds);
		return result;
	}

	/**
	 * 
	 * 
	 */
	@ApiOperation(value = "Updates the number of job executor threads for a worker", tags = {"Workers"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "workerId", value = "ID of the worker to update.", required = true, paramType = "path"),
		@ApiImplicitParam(name = "numThreads", value = "Number of threads to set for the worker.", required = true, paramType = "path")
	})
	@RequestMapping(value = "/worker/{workerId}/updateNumJobExecThreads/{numThreads}", method = POST)
	public @ResponseBody String updateWorkerNumJobExecThreads(
			@ApiIgnore final HttpSession session,
			@PathVariable String workerId,
			@PathVariable String numThreads) {
		
		log.info("*** REST CALL ***  updateWorkerNumJobExecThreads (workerId='"+workerId+"', numThreads='"+numThreads+"')...");
		
		try {
			int numThreadsInt = Integer.parseInt(numThreads);
			
			int cores = Runtime.getRuntime().availableProcessors();
			int maxRange = cores * 2;
			
			// Validate range
			if (numThreadsInt < 3 || numThreadsInt > maxRange) {
				log.warn("UI-VALUE-ERROR: numThreads (" + numThreadsInt + ") outside of allowable range (3 - " + maxRange + ")");
				return numThreadsInt + " outside of allowable range (3 - " + maxRange + ").  Worker has " + cores + " CPU cores.";
			}
			dbService.updateWorkerNumJobExecutorThreads(workerId, numThreadsInt);
		}
		catch (NumberFormatException e) {
			log.error("UI-VALUE-ERROR: Specified invalid number: " + numThreads);
			return "invalid number: " + numThreads;
		}
		catch (Exception e) {
			log.error("Unexpected error", e);
			return "update failed";
		}
		
		// Send out topic message, so worker can be notified,
		// and make change to its configuration
		//
		cwsConsoleService.sendWorkerConfigChangeTopicMessage();
		
		return "success";
	}
	
	
	/**
	 * Authenticates the User, as this passes through CWS security.
	 * Depending on the security scheme CWS is using,
	 * a cookie may be set in the response.
	 * 
	 * This cookie, can then be used to make future requests.
	 * 
	 */
	@ApiOperation(value = "Authenticates the user via GET.", tags = {"Security"}, produces = "application/json")
	@RequestMapping(value="/authenticate", method = GET)
	public @ResponseBody String authenticateViaGet(
			@ApiIgnore final HttpSession session) {
		log.debug("/authenticate call got through CWS security!");
		return "{\"status\" : \"SUCCESS\", \"session\" : \"" + session.getId() + "\"}";
	}
	
	
	/**
	 * Authenticates the User, as this passes through CWS security.
	 * Depending on the security scheme CWS is using,
	 * a cookie may be set in the response.
	 * 
	 * This cookie, can then be used to make future requests.
	 * 
	 */
	@ApiOperation(value = "Authenticates the user via POST.", tags = {"Security"}, produces = "application/json")
	@RequestMapping(value = "/authenticate", method = POST)
	public @ResponseBody String authenticateViaPost(
			@ApiIgnore final HttpSession session,
			HttpServletResponse response) {
		log.debug("/authenticate call got through CWS security!");
		return "{\"status\" : \"SUCCESS\", \"session\" : \"" + session.getId() + "\"}";
	}
	
	
	/**
	 * Validates CWS token (checks for expiration)
	 * 
	 */
	@ApiOperation(value = "Validates CWS token.", tags = {"Security"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "cwsToken", value = "CWS token to validate.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/validateCwsToken", method = POST)
	public @ResponseBody String validateCwsToken(
			@ApiIgnore final HttpSession session,
			HttpServletResponse response,
			@RequestParam String cwsToken) {
		log.trace("validateCwsToken... (cwsToken="+cwsToken+", session.id="+session.getId()+")");
		boolean isValid = session.getId().equals(cwsToken);
		log.trace("/validateCwsToken returning " + isValid);
		if (!isValid) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
		return isValid+"";
	}
	
	
	/**
	 * For testing purposes - if you want to send messages to the built-in ActiveMQ broker
	 * 
	 */
	@ApiOperation(value = "Posts a message to an AMQ queue.", tags = {"Messaging"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "payload", value = "Payload to post to the queue.", required = true, paramType = "query")
	})
	@RequestMapping(value = "/postAmqTopic", method = GET)
	public @ResponseBody String postAmqTopic(@RequestParam(value = "payload", required=true) final String payload) {
		log.debug("posting AMQ topic... payload: " + payload);
		jmsProcessInitiatorTemplate.send(new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				BytesMessage msg = session.createBytesMessage();
				msg.setStringProperty("key",   payload);
				return msg;
			}
		});
		return "posted to topic with content " + payload;
	}
	
	
	/**
	 * Utility method so that authenticated client (for example a project web page)
	 * can make a call to get data from an external resource.
	 * 
	 */
	@ApiOperation(value = "Makes an external HTTP GET request.", tags = {"External"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "url", value = "URL to make the GET request to.", required = true, paramType = "query"),
		@ApiImplicitParam(name = "acceptType", value = "Accept type for the request.", required = false, paramType = "query")
	})
	@RequestMapping(value = "/externalGetReq", method = GET)
	public @ResponseBody String externalGetReq(
			@RequestParam(value = "url", required=true) final String url,
			@RequestParam(value = "acceptType", required=false) final String acceptType) {
		log.debug("making external HTTP GET (acceptType=" + acceptType + ") request with URL: " + url);
		
		RestCallResult restCallResult = null;
		try {
			restCallResult = WebUtils.restCall(url, "GET", null, null, acceptType, null);
		} catch (Exception e) {
			log.error("Exception occurred while making external GET request to: " + url, e);
			return "ERROR";
		}
		if (restCallResult == null || restCallResult.getResponseCode() != 200) {
			return "ERROR";
		}
		return restCallResult.getResponse();
	}
	
	
	/**
	 * Utility method so that authenticated client (for example a project web page)
	 * can make a REST POST call to an external resource.
	 * 
	 * This call expects a parameter with a key of 'data' that holds the POST data body.
	 * 
	 */
	@ApiOperation(value = "Makes an external HTTP POST request.", tags = {"External"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "url", value = "URL to make the POST request to.", required = true, paramType = "query"),
		@ApiImplicitParam(name = "contentType", value = "Content type for the request.", required = false, paramType = "query")
	})
	@RequestMapping(value = "/externalPostReq", method = POST)
	public @ResponseBody String externalPostReq(
			HttpServletRequest request,
			@RequestParam(value = "url", required=true) final String url,
			@RequestParam(value = "contentType", required=false) final String contentType) {
		
		String postPayload = request.getParameter("data");
		log.debug("making external HTTP POST request with URL=" + url + ", contentType="+contentType+", and postPayload=" + postPayload);
		
		RestCallResult restCallResult = null;
		try {
			restCallResult = WebUtils.restCall(url, "POST", postPayload, null, null, contentType);
		} catch (Exception e) {
			log.error("Exception occurred while making external POST request to: " + url, e);
			return "ERROR";
		}
		if (restCallResult == null || restCallResult.getResponseCode() != 200) {
			log.error("REST POST FAILED.  Response is: " + restCallResult.getResponse() + ", message = " + restCallResult.getResponseMessage());
			return "ERROR";
		}
		return restCallResult.getResponse();
	}
	
	
	/**
	 * Utility method so that authenticated client (for example a project web page)
	 * can make a REST PUT call to an external resource.
	 * 
	 * This call expects a parameter with a key of 'data' that holds the PUT data body.
	 * 
	 */
	@ApiOperation(value = "Makes an external HTTP PUT request.", tags = {"External"}, produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "url", value = "URL to make the PUT request to.", required = true, paramType = "query"),
		@ApiImplicitParam(name = "contentType", value = "Content type for the request.", required = false, paramType = "query"),
		@ApiImplicitParam(name = "payload", value = "Payload to post to the queue.", required = true, paramType = "body")
	})
	@RequestMapping(value = "/externalPutReq", method = PUT)
	public @ResponseBody String externalPutReq(
			HttpServletRequest request,
			@RequestParam(value = "url", required=true) final String url,
			@RequestParam(value = "contentType", required=false) final String contentType,
			@RequestBody String payload) {
		
		log.debug("making external HTTP PUT request with URL=" + url + ", contentType="+contentType+", and payload=" + payload);
		
		RestCallResult restCallResult = null;
		try {
			restCallResult = WebUtils.restCall(url, "PUT", payload, null, null, contentType);
		} catch (Exception e) {
			log.error("Exception occurred while making external PUT request to: " + url, e);
			return "ERROR";
		}
		if (restCallResult == null || restCallResult.getResponseCode() != 200) {
			return "ERROR";
		}
		return restCallResult.getResponse();
	}

}