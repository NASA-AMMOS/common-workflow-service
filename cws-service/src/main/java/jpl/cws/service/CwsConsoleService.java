package jpl.cws.service;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import de.ruedigermoeller.serialization.FSTObjectInput;
import jpl.cws.core.code.CodeService;
import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.log.CwsEmailerService;
import jpl.cws.core.service.ProcessService;
import jpl.cws.core.service.SpringApplicationContext;
import jpl.cws.core.web.DiskUsage;
import jpl.cws.core.web.LogInfo;
import jpl.cws.core.web.WorkerInfo;
import jpl.cws.process.initiation.CwsProcessInitiator;
import jpl.cws.scheduler.*;
import jpl.cws.service.camunda.CamundaExecutionService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.*;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.io.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * cws-console service layer
 * <p>
 * Delegates to other services (e.g. cws-core CodeService) as necessary
 *
 * @author ghollins
 */
public class CwsConsoleService {
    private static final Logger log = LoggerFactory.getLogger(CwsConsoleService.class);

    @Autowired
    private CodeService cwsCodeService;
    @Autowired
    private JmsTemplate jmsCodeUpdateTopicTemplate;
    @Autowired
    private JmsTemplate jmsWorkerLogCleanupTopicTemplate;
    @Autowired
    private JmsTemplate jmsSystemShutdownTopicTemplate;
    @Autowired
    private JmsTemplate jmsWorkerConfigChangeTopicTemplate;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;
    @Autowired
    protected CamundaExecutionService cwsExecutionService;
    @Autowired
    private SpringApplicationContext springApplicationContext;
    @Autowired
    private SchedulerDbService schedulerDbService;
    @Autowired
    protected SchedulerQueueUtils cwsSchedulerUtils;
    @Autowired
    private ProcessService cwsProcessService;
    @Autowired
    private CwsEmailerService cwsEmailerService;
    @Autowired
    private RuntimeService runtimeService;

    @Value("${cws.install.dir}")
    private String cwsInstallDir;
    @Value("${cws.history.days.to.live}")
    private String historyDaysToLive;

    private String CWS_HOME;

    private static Pattern cmdLineExecPattern = Pattern.compile(
            "^(.*\\bjpl.cws.task.CmdLineExecTask\\b.*)(camunda:topic=\".*?\")(.*)$");
    private static Pattern emptyFieldPattern = Pattern.compile(
            "^\\s*<camunda:field name=\".*?\"\\s*/>$");
    private static Pattern userTaskPattern = Pattern.compile(
            "^(?!.*?camunda:asyncAfter=\"true\")(\\s*<bpmn2?:userTask.*)(id=\".*?\")(.*)$");

    public CwsConsoleService() {
        log.trace("CwsConsoleService constructor...");
    }

    /**
     * Returns the latest successfully compiled code in the database.
     */
    public String getLatestCode() {
        return cwsCodeService.getLatestCode();
    }

    /**
     * Returns the latest "in progress" code in the database.
     */
    public String getLatestInProgressCode() {
        return cwsCodeService.getLatestInProgressCode();
    }

    /**
     *
     */
    public void sendWorkerConfigChangeTopicMessage() {
        try {
            log.info("sending worker config change topic message...");
            jmsWorkerConfigChangeTopicTemplate.send(new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    return session.createBytesMessage();
                }
            });
        } catch (Throwable e) {
            cwsEmailerService.sendNotificationEmails("CWS JMS Error",
                    "Severe Error!\n\nCould not access AMQ.\n\nDetails: " + e.getMessage());
            log.error("Could not access AMQ.", e);

            throw e;
        }
    }


    /**
     * Returns CWS_HOME environment variable
     */
    public String getCwsHome() throws Exception {
        if (CWS_HOME != null) {
            return CWS_HOME;
        }
        CWS_HOME = System.getenv("CWS_HOME");
        if (CWS_HOME == null || CWS_HOME.isEmpty()) {
            log.error("no CWS_HOME environmental variable set!!");
            throw new Exception("no CWS_HOME environmental variable set!");
        }
        return CWS_HOME;
    }


    /**
     *
     */
    public String deployProcessDefinitionXmlFile(File bpmnFile) throws Exception {

        BpmnModelInstance model = Bpmn.readModelFromFile(bpmnFile);
        ModelElementType processType = model.getModel().getType(Process.class);
        Collection<ModelElementInstance> processInstances = model.getModelElementsByType(processType);
        String procDefKey = processInstances.iterator().next().getAttributeValue("id");

        File tempFile = new File(getCwsHome() + "/bpmn/" + bpmnFile.getName() + ".tmp");
        FileUtils.deleteQuietly(tempFile); // make sure any old tmp file is deleted first
        FileUtils.moveFile(bpmnFile, tempFile);

        // Make a second pass, to swap in topic names
        BufferedReader reader = null;

        // Translate, and write out to final file (under cws/bpmn directory)
        //
        File finalFile = new File(getCwsHome() + "/bpmn/" + bpmnFile.getName());

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(finalFile));
            reader = new BufferedReader(new FileReader(tempFile));
            String line = reader.readLine();

            // Scan each line using the compiled patterns, making adjustments where necessary to produce
            // a valid final XML file for deployment
            while (line != null) {
                Matcher cmdLineExecMatcher = cmdLineExecPattern.matcher(line);
                Matcher emptyFieldMatcher = emptyFieldPattern.matcher(line);
                Matcher userTaskFieldMatcher = userTaskPattern.matcher(line);

                // Replace topic name if external task uses a 'jpl.cws.task.CmdLineExecTask' modelTemplate
                // (this is the value set by the modeler in the case of Command Line Execution tasks.
                if (cmdLineExecMatcher.matches()) {
                    bw.write(cmdLineExecMatcher.group(1) + "camunda:topic=\"" + procDefKey + "\"" + cmdLineExecMatcher.group(3));
                }
                // Filter out empty camunda:field tags which can sneak in for REST GET/POST tasks
                else if (emptyFieldMatcher.matches()) {
                    log.debug("filtered out empty tag: " + line);
                }
                // Always add camunda:asyncAfter="true" to User Tasks
                else if (userTaskFieldMatcher.matches()) {
                    bw.write(userTaskFieldMatcher.group(1) + userTaskFieldMatcher.group(2) + " camunda:asyncAfter=\"true\"" + userTaskFieldMatcher.group(3));
                } else {
                    bw.write(line);
                }
                bw.newLine();
                line = reader.readLine();
            }
        } catch (Exception e) {
            log.error("problem parsing BPMN XML...", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("Problem when closing BufferedReader", e);
            }
            try {
                bw.close();
            } catch (IOException e) {
                log.error("Problem when closing BufferedWriter", e);
            }
        }

        model = Bpmn.readModelFromFile(finalFile);

        return deployProcessDefinition(finalFile, model, bpmnFile.getName());
    }


    /**
     * Deploys a process definition
     */
    public String deployProcessDefinition(File file, BpmnModelInstance model, String origFileName) throws Exception {

        // Read data out of InputStream into a String
        String bpmnXml;

        try (FileInputStream fs = new FileInputStream(file)) {
            bpmnXml = IOUtils.toString(fs, "UTF-8");
        } catch (IOException e) {
            return "ERROR: " + e.getMessage();
        }

        return deployProcessDefinition(bpmnXml, model, origFileName);
    }


    /**
     * Deploys a process definition
     *
     * @throws Exception
     */
    public String deployProcessDefinition(String bpmnXml, BpmnModelInstance model, String origFileName) throws Exception {
        try {
            String url = "https://github.com/NASA-AMMOS/common-workflow-service/wiki";
            //
            // Validate that process definition uses an async start event.
            // This is preferable so that we don't spawn a lot of threads
            // that hang around and don't return immediately.
            //
            if (!containsProcessStartAsync(model)) {
                log.warn("BPMN XML does not contain async start event!");
                return "ERROR: Make sure your process has an \"Asynchronous Before\" startEvent. Please refer to the <a href=\" "
                        + url + "\" target =\"_blank\">wiki</a>.";
            }

            // Validate that process definition has a name.
            if (!hasName(model)) {
                log.warn("BPMN model does not have a name!");
                return "ERROR: Make sure your process has a name. Please refer to the <a href=\" " + url
                        + "\" target =\"_blank\">wiki</a>.";
            }

            // Validate that process id contains only alphanumeric characters
            // and underscore
            if (!noInvalidCharacters(model)) {
                log.warn("BPMN model process id contains invalid character(s)!");
                return "ERROR: Your process id contains invalid character(s). Please refer to the <a href=\" " + url
                        + "\" target =\"_blank\">wiki</a>.";
            }

            // Validate that process definition has 'Is Executable' checked.
            if (!isExecutable(model)) {
                log.warn("BPMN model is not executable");
                return "ERROR: Make sure your process has \"Is Executable\" checked. Please refer to the <a href=\" "
                        + url + "\" target =\"_blank\">wiki</a>.";
            }

            // Get DeploymentBuilder from repository service
            //
            DeploymentBuilder deployment = repositoryService.createDeployment();

            // give deployment the name of the files, for easy identification
            deployment.name(origFileName);
            // configure with source data
            deployment.addString(origFileName, bpmnXml);
            // configure to filter out duplicates
            deployment.enableDuplicateFiltering(true);

            // Deploy process definition
            //
            log.debug("About to deploy " + origFileName + "...");
            Deployment resultingDeployment = deployment.deploy();
            log.info(origFileName + " deployed.  " + resultingDeployment.getId() + ", " + resultingDeployment.getName()
                    + ", " + resultingDeployment.getDeploymentTime());

            // Update existing worker_proc_def rows in DB
            //
            ProcessDefinition deployedProcDef = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(resultingDeployment.getId()).list().iterator().next();
            for (Map<String, Object> worker : schedulerDbService.getWorkers()) {
                log.trace("UPDATING WORKER_PROC_DEF_ROW: " + worker.get("id").toString() + ", "
                        + deployedProcDef.getKey() + ", " + deployedProcDef.getDeploymentId());
                schedulerDbService.updateWorkerProcDefDeploymentId(worker.get("id").toString(),
                        deployedProcDef.getKey(), deployedProcDef.getDeploymentId());
            }

            repositoryService.updateProcessDefinitionHistoryTimeToLive(deployedProcDef.getId(), Integer.parseInt(historyDaysToLive));

            // Notify workers that there has been a change
            // to the set of deployed process definitions
            //
            sendWorkerConfigChangeTopicMessage();

        } catch (Exception e) {
            log.error("Something went wrong in deploying the process definition: ", e);
            return "ERROR: Failed to deploy process definition XML (" + origFileName
                    + ") and/or notify workers. (Message = '" + e.getMessage() + "')";
        }

        log.info("Deployed process definition: '" + origFileName + "'. Newly deployed file can be found under " + getCwsHome() + "/bpmn");

        return null; // no error to report
    }


    // Note: this method uses isCamundaAsync() in addition to
    // isCamundaAsyncBefore()
    // at the suggestion of the developer of this method.
    // This allows it to find the 'async:camunda' attribute in the .bpmn files
    // from the modeler,
    // which has since been deprecated in place of 'asyncBefore:camunda'
    //
    @SuppressWarnings("deprecation")
    private boolean containsProcessStartAsync(BpmnModelInstance model) {
        ModelElementType startEventType = model.getModel().getType(StartEvent.class);
        Collection<ModelElementInstance> startEventInstances = model.getModelElementsByType(startEventType);
        boolean acc = true;
        for (ModelElementInstance event : startEventInstances) {
            StartEventImpl startEvent = (StartEventImpl) event;
            acc = (acc && (startEvent.isCamundaAsync() || startEvent.isCamundaAsyncBefore()));
        }
        return acc;
    }

    private boolean hasName(BpmnModelInstance model) {
        ModelElementType processType = model.getModel().getType(Process.class);
        Collection<ModelElementInstance> processInstances = model.getModelElementsByType(processType);
        return (processInstances.iterator().next().getAttributeValue("name") != null);
    }

    private boolean noInvalidCharacters(BpmnModelInstance model) {
        ModelElementType processType = model.getModel().getType(Process.class);
        Collection<ModelElementInstance> processInstances = model.getModelElementsByType(processType);
        String pattern = "[\\w]+";
        return processInstances.iterator().next().getAttributeValue("id").matches(pattern);
    }

    private boolean isExecutable(BpmnModelInstance model) {
        ModelElementType processType = model.getModel().getType(Process.class);
        Collection<ModelElementInstance> processInstances = model.getModelElementsByType(processType);
        return (processInstances.iterator().next().getAttributeValue("isExecutable").equals("true"));
    }

    /**
     * Validates the specified code, and sends message to topic if valid, so
     * that listeners may know that code has been updated.
     *
     * @return true if valid, false otherwise
     */
    public String validateAndPersistCode(String code) {
        String codeErrors = cwsCodeService.validateAndPersistCode(code);

        // If code was valid and persisted, send message to topic
        // so Workers (CodeUpdateListener) will be notified of code update
        if (codeErrors == null) {
            sendCodeUpdatedTopicMessage();
        }

        return codeErrors;
    }

    /**
     * Init the specified code, and sends message to topic if valid, so that
     * listeners may know that code has been updated.
     *
     * @return true if valid, false otherwise
     */
    public String doSystemShutdown() {

        sendSystemShutdownTopicMessage();

        return "\nShutting down system now...\n";
    }

    public CwsProcessInitiator getProcessInitiatorById(String initiatorId) {
        CwsProcessInitiator initiator = (CwsProcessInitiator) SpringApplicationContext.getBean(initiatorId);
        return initiator;
    }

    public void replaceInitiatorBean(String springBeanKey, CwsProcessInitiator newInitiator) {
        log.debug("replaceInitiatorBean initiator: " + springBeanKey + " ...");
        springApplicationContext.replaceBean(springBeanKey, null, newInitiator.getPropertyValues(),
                newInitiator.getClass());
    }

    public void removeBean(String beanName) {
        springApplicationContext.removeBean(beanName);
    }

    public <T> String[] getBeanNamesOfType(Class<T> type) {
        return springApplicationContext.getBeanDefinitionNamesOfType(type);
    }

    public <T> void removeAllBeansOfType(Class<T> type) {
        springApplicationContext.removeAllBeansOfType(type);
    }


    private void getHistoryVarDetails(List<HistoryDetail> history, String processInstanceId) {

        List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery()
                .processInstanceId(processInstanceId)
                .list();

        for (HistoricDetail detail : historicDetails) {

            if (detail instanceof HistoricVariableUpdate) {

                HistoricDetailVariableInstanceUpdateEntity variable = (HistoricDetailVariableInstanceUpdateEntity) detail;

                String message = "";

                if (variable.getSerializerName().equals("json")) {

                    // TODO: Update this to be a collapsible table
                    message = "Setting (json) " + variable.getVariableName() + " = " + variable.getValue();
                } else {
                    message = "Setting (" + variable.getTypeName() + ") " + variable.getVariableName() + " = " + variable.getValue();
                }

                HistoryDetail historyDetail = new HistoryDetail(variable.getTime(), "VarUpdate", variable.getActivityInstanceId().split(":")[0], message);

                history.add(historyDetail);
            }
        }
    }

    private void getHistoryIncidentDetails(List<HistoryDetail> history, String processInstanceId) {

        List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery()
                .processInstanceId(processInstanceId)
                .list();

        for (HistoricIncident incident : historicIncidents) {

            String message = incident.getIncidentType() + ": " + incident.getIncidentMessage();

            if (incident.isOpen()) {
                message += " (Incident Open)";
            }

            if (incident.isDeleted()) {
                message += " (Incident Deleted)";
            }

            if (incident.isResolved()) {
                message += " (Incident Resolved)";
            }

            HistoryDetail historyDetail = new HistoryDetail(incident.getCreateTime(), "Incident", incident.getActivityId(), message);

            history.add(historyDetail);
        }
    }

    private void getHistoryActivityDetails(List<HistoryDetail> history, String processInstanceId) {

        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        for (HistoricActivityInstance activity : historicActivities) {

            String message = "Started " + activity.getActivityType();

            if (activity.getActivityName() != null) {
                message += " with description \"" + activity.getActivityName() + "\"";
            }

            HistoryDetail historyDetail = new HistoryDetail(activity.getStartTime(), "ProcessFlow", activity.getActivityId(), message);
            history.add(historyDetail);

            if (activity.getEndTime() != null) {

                String messageEnd = "Ended " + activity.getActivityType();

                if (activity.getDurationInMillis() != null) {
                    messageEnd += " with duration " + activity.getDurationInMillis() + " msec";
                }

                HistoryDetail historyDetailEnd = new HistoryDetail(activity.getEndTime(), "ProcessFlow", activity.getActivityId(), messageEnd);

                history.add(historyDetailEnd);
            }
        }
    }

    private void getHistoryUserOperationLog(List<HistoryDetail> history, String processInstanceId) {

        List<UserOperationLogEntry> userOperations = historyService.createUserOperationLogQuery()
                .processInstanceId(processInstanceId)
                .list();

        for (UserOperationLogEntry userOperation : userOperations) {

            String message = userOperation.getEntityType() + ": " + userOperation.getOperationType() + ". Property \"" + userOperation.getProperty() + "\" changed from " + userOperation.getOrgValue() + " to " + userOperation.getNewValue();

            HistoryDetail historyDetail = new HistoryDetail(userOperation.getTimestamp(), "UserOp", "N/A", message);

            history.add(historyDetail);
        }
    }

    public LogHistory getHistoryForProcess(String processInstanceId) {

        LogHistory history = new LogHistory();

        history.procInstId = processInstanceId;

        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

        if (instance == null) {

            // History may have been cleared and no longer exists so return empty history
            return history;
        }

        history.procDefKey = instance.getProcessDefinitionKey();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        history.startTime = dateFormat.format(instance.getStartTime());

        if (instance.getEndTime() != null) {
            history.endTime = dateFormat.format(instance.getEndTime());
        }

        history.state = instance.getState();

        if (instance.getDurationInMillis() != null) {
            history.duration = instance.getDurationInMillis();
        }

        history.inputVariables = getInputVariablesForProcess(processInstanceId);
        history.outputVariables = getOutputVariablesForProcess(processInstanceId);

        getHistoryIncidentDetails(history.details, processInstanceId);
        getHistoryActivityDetails(history.details, processInstanceId);
        getHistoryVarDetails(history.details, processInstanceId);
        getHistoryUserOperationLog(history.details, processInstanceId);

        return history;
    }

    public Map<String, String> getInputVariablesForProcess(String processInstanceId) {
        Map<String, String> inputVarMap = new HashMap<String, String>();

        List<HistoricVariableInstance> historicVariableInstances= historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list();

        if (historicVariableInstances.isEmpty()) {
            return inputVarMap;
        }

        for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
            String varName = historicVariableInstance.getName();
            if(!(varName.toUpperCase().startsWith("TASK_") && (varName.toUpperCase().endsWith("_IN") || varName.toUpperCase().endsWith("_OUT"))) && !(varName.toUpperCase().startsWith("OUTPUT_"))) {
                String varType = historicVariableInstance.getTypeName();
                //if varType is not a file, then get the value as a string and put it in the outputVarMap
                if (varType == null || !varType.equals("file")) {
                    Object objValue = historicVariableInstance.getValue();
                    if (objValue == null) {
                        inputVarMap.put(varName + " (" + varType + ")", null);
                    } else {
                        String varValue = historicVariableInstance.getValue().toString();
                        inputVarMap.put(varName + " (" + varType + ")", varValue);
                    }
                } else {
                    //the variable is a file.
                    //we need to get the file name and the contents of the file and put them in the outputVarMap
                    TypedValue typedValue = historicVariableInstance.getTypedValue();
                    if (typedValue instanceof FileValue) {
                        FileValue fileValue = (FileValue) typedValue;
                        String fileName = fileValue.getFilename();
                        String mimeType = fileValue.getMimeType();
                        if (mimeType.contains("image")) {
                            InputStream fileInputStream = fileValue.getValue();
                            String encodedString = "data:" + mimeType + ";base64, ";
                            try {
                                byte[] sourceBytes = IOUtils.toByteArray(fileInputStream);
                                encodedString += Base64.getEncoder().encodeToString(sourceBytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            inputVarMap.put(varName + " (" + varType + ", image)", encodedString);
                        } else {
                            inputVarMap.put(varName + " (" + varType + ")", fileName);
                        }
                    }
                }
            }
        }
        return inputVarMap;
    }

    public Map<String, String> getOutputVariablesForProcess(String processInstanceId) {
        Map<String, String> outputVarMap = new HashMap<String, String>();

        List<HistoricVariableInstance> historicVariableInstances= historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list();

        if (historicVariableInstances.isEmpty()) {
            return outputVarMap;
        }

        for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
            String varName = historicVariableInstance.getName();
            String varActivity = historicVariableInstance.getActivityInstanceId().split(":")[0];
            if(!(varName.toUpperCase().startsWith("TASK_") && (varName.toUpperCase().endsWith("_IN") || varName.toUpperCase().endsWith("_OUT"))) && (varName.toUpperCase().startsWith("OUTPUT_"))) {
                String varType = historicVariableInstance.getTypeName();
                //if varType is not a file, then get the value as a string and put it in the outputVarMap
                if (varType == null || !varType.equals("file")) {
                    Object objValue = historicVariableInstance.getValue();
                    if (objValue == null) {
                        outputVarMap.put(varName + " (" + varType + ")", null);
                    } else {
                        String varValue = historicVariableInstance.getValue().toString();
                        outputVarMap.put(varName + " (" + varType + ")", varValue);
                    }
                } else {
                    //the variable is a file.
                    //we need to get the file name and the contents of the file and put them in the outputVarMap
                    TypedValue typedValue = historicVariableInstance.getTypedValue();
                    if (typedValue instanceof FileValue) {
                        FileValue fileValue = (FileValue) typedValue;
                        String fileName = fileValue.getFilename();
                        String mimeType = fileValue.getMimeType();
                        if (mimeType.contains("image")) {
                            InputStream fileInputStream = fileValue.getValue();
                            String encodedString = "data:" + mimeType + ";base64, ";
                            try {
                                byte[] sourceBytes = IOUtils.toByteArray(fileInputStream);
                                encodedString += Base64.getEncoder().encodeToString(sourceBytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            outputVarMap.put(varName + " (" + varType + ", " + mimeType + ")", encodedString);
                        } else {
                            outputVarMap.put(varName + " (" + varType + ")", fileName);
                        }
                    }
                }
            }
        }
        return outputVarMap;
    }

    public List<ExternalWorker> getExternalWorkersUiDTO() {
        List<ExternalWorker> workers = new ArrayList<ExternalWorker>();

        List<Map<String, Object>> workerRows = schedulerDbService.getExternalWorkers();

        for (Map<String, Object> workerRow : workerRows) {
            String workerId = workerRow.get("id").toString();
            String workerName = workerRow.get("name").toString();
            String workerHostname = workerRow.get("hostname").toString();

            // Create new worker object if necessary
            //
            ExternalWorker worker = new ExternalWorker(workerId, workerName, workerHostname);

            // Get active topics
            //
            Object activeTopicsObj = workerRow.get("activeTopics");
            if (activeTopicsObj != null) {
                worker.setActiveTopics(activeTopicsObj.toString());
            }

            // Get current topic
            //
            Object currentTopicObj = workerRow.get("currentTopic");
            if (currentTopicObj != null) {
                worker.setCurrentTopic(currentTopicObj.toString());
            }

            // Get current command
            //
            Object currentCommandObj = workerRow.get("currentCommand");
            if (currentCommandObj != null) {
                worker.setCurrentCommand(currentCommandObj.toString());
            }

            // Get current working dir
            //
            Object currentWorkingDirObj = workerRow.get("currentWorkingDir");
            if (currentWorkingDirObj != null) {
                worker.setCurrentWorkingDir(currentWorkingDirObj.toString());
            }

            // Get and set worker.lastHeartbeatTime
            //
            Timestamp workerLastHeartbeatTime = (Timestamp) workerRow.get("last_heartbeat_time");
            log.trace("worker " + workerId + ", got last_heartbeat_time = " + workerLastHeartbeatTime);
            worker.setLastHeartbeatTime(workerLastHeartbeatTime);

            log.trace("adding external worker to uiDTO: " + workerId);
            workers.add(worker);
        }

        return workers;
    }

    /**
     *
     */
    public List<Worker> getWorkersUiDTO(List<ProcessDefinition> procDefs) {
        List<Worker> workers = new ArrayList<Worker>();

        List<Map<String, Object>> workerRows = schedulerDbService.getWorkers();

        List<Map<String, Object>> workerProcDefRows = schedulerDbService.getWorkerProcDefRows();

        for (Map<String, Object> workerRow : workerRows) {
            String workerId = workerRow.get("id").toString();
            String workerName = workerRow.get("name").toString();
            String workerType = workerRow.get("cws_worker_type").toString();

            // Get worker install type
            //
            Object workerInstallTypeObj = workerRow.get("cws_install_type");
            String workerInstallType = "unknown";
            if (workerInstallTypeObj == null) {
                log.error("'cws_install_type' value for worker '" + workerId + "' was null!");
            } else {
                workerInstallType = workerInstallTypeObj.toString();
            }

            // Get worker status
            //
            Object workerStatusObj = workerRow.get("status");
            String workerStatus = "unknown";
            if (workerStatusObj == null) {
                log.error("'status' value for worker '" + workerId + "' was null!");
            } else {
                workerStatus = workerStatusObj.toString();
            }

            // Create new worker object if necessary
            //
            Worker worker = new Worker(workerId, workerName, workerInstallType, workerType, workerStatus);

            // Get and set worker.lastHeartbeatTime
            //
            Timestamp workerLastHeartbeatTime = (Timestamp) workerRow.get("last_heartbeat_time");
            log.trace("worker " + workerId + ", got last_heartbeat_time = " + workerLastHeartbeatTime);
            worker.setLastHeartbeatTime(workerLastHeartbeatTime);

            // Get and set worker.jobExecutorMaxPoolSize
            //
            worker.setJobExecutorMaxPoolSize((Integer) workerRow.get("job_executor_max_pool_size"));

            Map<String, Integer> procInstanceLimits = worker.getProcInstanceLimits();

            for (Map<String, Object> row : workerProcDefRows) {

                Boolean acceptingNew = (Boolean) row.get("accepting_new");
                if (acceptingNew) {
                    String procDefWorkerId = row.get("worker_id").toString();
                    if (procDefWorkerId.equals(workerId)) {
                        String procDefKey = row.get("proc_def_key").toString();
                        Integer limit = Integer.parseInt(row.get("max_instances").toString());
                        procInstanceLimits.put(procDefKey, limit);
                    }
                }
            }

            for (ProcessDefinition procDef : procDefs) {
                if (!procInstanceLimits.containsKey(procDef.getKey())) {
                    procInstanceLimits.put(procDef.getKey(), null);
                }
            }

            // set updated procInstanceLimits map back to worker object
            worker.setProcInstanceLimits(procInstanceLimits);

            log.trace("adding worker to uiDTO: " + workerId);
            workers.add(worker);
        }

        return workers;
    }

    /**
     * Returns the title representing the statistics of the workers
     *
     * @return
     */
    public String getWorkersTitle() {
        try {
            List<Map<String, Object>> rows = schedulerDbService.getWorkersStats();

            Integer nUp = 0, nDown = 0;

            for (Map<String, Object> row : rows) {

                String status = row.get("status").toString();

                Integer count = Integer.parseInt(row.get("cnt").toString());

                if (status.equals("up")) {
                    nUp = count;
                } else if (status.equals("down")) {
                    nDown = count;
                }
            }

            return (nUp + nDown) + " Workers (" + nUp + " up, " + nDown + " down)";
        } catch (Exception e) {
            return "Workers";
        }
    }

    /**
     *
     */
    public Map<String, String> getProcessInstanceStats(String lastNumHours) {
        List<Map<String, Object>> rows = schedulerDbService.getProcessInstanceStats(lastNumHours);

        Map<String, String> ret = new HashMap<String, String>();
        for (Map<String, Object> row : rows) {
            String key = row.get("proc_def_key").toString();
            String status = row.get("status").toString();

            if (status.equals("complete")) {
                key = "numCompleted_" + key;
            } else if (status.equals("running")) {
                key = "numRunning_" + key;
            } else if (status.equals("pending")) {
                key = "numPending_" + key;
            }

            ret.put(key, row.get("cnt").toString());
        }

        return ret;
    }

    /**
     *
     */
    public Map<String, String> getWorkerNumRunningProcs() {
        List<Map<String, Object>> rows = schedulerDbService.getWorkerNumRunningProcs();

        Map<String, String> ret = new HashMap<String, String>();
        for (Map<String, Object> row : rows) {
            String key = row.get("id").toString();
            ret.put(key, row.get("cnt").toString());
        }

        return ret;
    }

    public DiskUsage getDiskUsage() throws Exception {

        DiskUsage diskUsage = new DiskUsage();

        try {

            diskUsage.databaseSize = schedulerDbService.getDbSize();

            List<Map<String, Object>> rows = schedulerDbService.getDiskUsage();

            for (Map<String, Object> row : rows) {

                String id = row.get("id").toString();
                String name = row.get("name").toString();
                String installType = row.get("cws_install_type").toString();
                Object diskFreeBytes = row.get("disk_free_bytes");

                if (installType.contains("console")) {

                    name = "Console (" + name + ")";
                }

                WorkerInfo workerInfo = new WorkerInfo(name, diskFreeBytes);

                // Get logs
                List<Map<String, Object>> logRows = schedulerDbService.getLogUsage(id);

                for (Map<String, Object> logRow : logRows) {

                    String filename = logRow.get("filename").toString();
                    Object size = logRow.get("size_bytes");

                    workerInfo.getLogs().add(new LogInfo(filename, size));
                }

                diskUsage.workers.add(workerInfo);
            }
        } catch (Exception e) {
            log.error("Error getting system disk usage.", e);

            throw e;
        }

        return diskUsage;
    }

    public void cleanupElasticsearch() {

        try {

            log.debug("Cleaning up elasticsearch...");

            java.lang.Process p = Runtime.getRuntime().exec(cwsInstallDir + "/clean_es_history.sh");

            // Wait for the process to complete
            //
            p.waitFor();

            if (p.exitValue() != 0) {

                log.error("Elasticsearch cleanup failed.  Exit code: " + p.exitValue());
            }

            log.debug("Elasticsearch cleanup done!");

        } catch (Exception e) {
            log.error("error: ", e);
        }
    }

    /*
     * Return JSON key values of process status e.g. {PD1: {errors:4,
     * pending:3,... },...}
     */
    public Map<String, Map<String, String>> getProcessInstanceStatsJSON(String lastNumHours) {

        List<Map<String, Object>> rows = schedulerDbService.getProcessInstanceStats(lastNumHours);

        Map<String, Map<String, String>> ret = new HashMap<String, Map<String, String>>();

        for (Map<String, Object> row : rows) {
            String procDefKey = row.get("proc_def_key").toString();

            String status = row.get("status").toString();

            // if the key is new
            if (!ret.containsKey(procDefKey)) {
                Map<String, String> statusObject = new HashMap<String, String>();
                statusObject.put(status, row.get("cnt").toString());

                ret.put(procDefKey, statusObject);
            } else { // existing proc_def_key
                ret.get(procDefKey).put(status, row.get("cnt").toString());
            }
        }

        return ret;
    }

    /*
     * Return summary of proc_def_key, business_key pair
     *
     * e.g. {fail: 100, complete: 30, pending: 10...}
     */
    public Map<String, Integer> getStatsByBusinessKey(String procDefKey, String businessKey) {

        List<Map<String, Object>> rows = schedulerDbService.getStatusByBusinessKey(procDefKey, businessKey);

        Map<String, Integer> ret = new HashMap<>();

        // Make sure we got a result
        if (!rows.isEmpty()) {
            // Pull it out of the list, we just want the single JSON object

            for (Map<String, Object> row : rows) {
                ret.put(row.get("status").toString(), Integer.parseInt(row.get("cnt").toString()));
            }
        }
        return ret;
    }


    /*
     *
     *
     */
    public JsonArray getPendingProcessesJSON() throws Exception {

        List<Map<String, Object>> rows = schedulerDbService.getPendingProcessInstances();

        JsonArray ja = new JsonArray();

        for (Map<String, Object> row : rows) {
            String uuid = row.get("uuid").toString();
            String procDefKey = row.get("proc_def_key").toString();
            String createdTime = row.get("created_time").toString();

            JsonObject jo = new JsonObject();

            jo.addProperty("uuid", uuid);
            jo.addProperty("created_time", createdTime);
            jo.addProperty("proc_def_key", procDefKey);

            try {
                // Get process variables as a map
                //
                byte[] procVarsAsBytes = (byte[]) row.get("proc_variables");
                FSTObjectInput in = new FSTObjectInput(new ByteArrayInputStream(procVarsAsBytes));
                Map<String, Object> procVars = (Map<String, Object>) in.readObject();
                in.close();

                if (procVars == null) {
                    procVars = new HashMap<String, Object>();
                }

                log.trace("procVars: " + procVars);

                JsonObject vars = new JsonObject();

                for (String varName : procVars.keySet()) {
                    Object value = procVars.get(varName);

                    if (value != null) {
                        vars.addProperty(varName, value.toString());
                    } else {
                        vars.add(varName, JsonNull.INSTANCE);
                    }
                }

                jo.add("variables", vars);
            } catch (Exception e) {

                log.error("getPendingProcessesJSON could not parse proc_variables", e);

                throw e;
            }

            ja.add(jo);
        }

        return ja;
    }

    /**
     *
     */
    public String getProcInstStatusJson(String uuidOrProcInstId) {
        Map<String, Object> data = cwsProcessService.getProcInstStatusMap(uuidOrProcInstId);
        return new GsonBuilder().setPrettyPrinting().create().toJson(data);
    }

    /**
     *
     */
    public List<CwsProcessInstance> getFilteredProcessInstancesCamunda(String superProcInstId, String procInstId, String procDefKey,
                                                                       String status, String minDate, String maxDate, String dateOrderBy, int page) {

        List<CwsProcessInstance> instances = new ArrayList<CwsProcessInstance>();

        List<Map<String, Object>> rows = schedulerDbService.getFilteredProcessInstances(superProcInstId, procInstId, procDefKey, status,
                minDate, maxDate, dateOrderBy, page);

        for (Map<String, Object> row : rows) {
            String uuidObj = (String) row.get("uuid");
            String procDefKeyObj = (String) row.get("proc_def_key");
            String procInstIdObj = (String) row.get("proc_inst_id");
            String superProcInstIdObj = (String) row.get("super_proc_inst_id");
            String statusObj = (String) row.get("status");
            String initiationKeyObj = (String) row.get("initiation_key");
            Timestamp createdTimestampObj = (Timestamp) row.get("created_time");
            Timestamp updatedTimestampObj = (Timestamp) row.get("updated_time");
            String claimedByWorker = (String) row.get("claimed_by_worker");
            String startedByWorker = (String) row.get("started_by_worker");
            Timestamp procStartTime = (Timestamp) row.get("proc_start_time");
            Timestamp procEndTime = (Timestamp) row.get("proc_end_time");
            Map<String, String> inputVars;
            Map<String, String> outputVars;
            if (procInstIdObj != null) {
                inputVars = getInputVariablesForProcess(procInstIdObj.toString());
                outputVars = getOutputVariablesForProcess(procInstIdObj.toString());
            } else {
                inputVars = new HashMap<String, String>();
                outputVars = new HashMap<String, String>();
            }
            CwsProcessInstance instance = new CwsProcessInstance(uuidObj == null ? null : uuidObj.toString(),
                    procDefKeyObj == null ? null : procDefKeyObj.toString(),
                    procInstIdObj == null ? null : procInstIdObj.toString(),
                    superProcInstIdObj == null ? null : superProcInstIdObj.toString(),
                    statusObj == null ? null : statusObj,
                    initiationKeyObj == null ? null : initiationKeyObj,
                    createdTimestampObj == null ? null : createdTimestampObj,
                    updatedTimestampObj == null ? null : updatedTimestampObj,
                    claimedByWorker == null ? null : claimedByWorker, startedByWorker == null ? null : startedByWorker,
                    procStartTime == null ? null : procStartTime, procEndTime == null ? null : procEndTime,
                    inputVars, outputVars);
            instances.add(instance);
        }

        return instances;
    }

    /**
     * Post to a message broker topic, to notify listeners that it needs to cleanup the logs
     */
    public void sendWorkerLogCleanupTopicMessage() {
        log.info("sending worker log cleanup topic message...");
        try {
            jmsWorkerLogCleanupTopicTemplate.send(new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    BytesMessage msg = session.createBytesMessage();
                    msg.setIntProperty("historyDaysToLive", Integer.parseInt(historyDaysToLive));
                    return msg;
                }
            });
        } catch (Throwable e) {
            cwsEmailerService.sendNotificationEmails("CWS JMS Error",
                    "Severe Error!\n\nCould not access AMQ.\n\nDetails: " + e.getMessage());
            log.error("Could not access AMQ.", e);

            throw e;
        }
    }

    /**
     * Post to a message broker topic, to notify listeners that the code has
     * been updated.
     */
    private void sendCodeUpdatedTopicMessage() {
        log.info("sending code updated topic message...");
        try {
            jmsCodeUpdateTopicTemplate.send(new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    return session.createBytesMessage();
                }
            });
        } catch (Throwable e) {
            cwsEmailerService.sendNotificationEmails("CWS JMS Error",
                    "Severe Error!\n\nCould not access AMQ.\n\nDetails: " + e.getMessage());
            log.error("Could not access AMQ.", e);

            throw e;
        }
    }

    /**
     * Post to a message broker topic, to notify listeners to shutdown.
     */
    private void sendSystemShutdownTopicMessage() {
        log.info("sending system shutdown topic message...");
        try {
            jmsSystemShutdownTopicTemplate.send(new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    return session.createBytesMessage();
                }
            });
        } catch (Throwable e) {
            cwsEmailerService.sendNotificationEmails("CWS JMS Error",
                    "Severe Error!\n\nCould not access AMQ.\n\nDetails: " + e.getMessage());
            log.error("Could not access AMQ.", e);

            throw e;
        }
    }
}
