package jpl.cws.engine;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import javax.jms.Session;

import com.google.gson.Gson;
import jpl.cws.core.CmdLineInputFields;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.camunda.bpm.application.PostDeploy;
import org.camunda.bpm.application.PreUndeploy;
import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.ProcessEngineImpl;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.context.CoreExecutionContext;
import org.camunda.bpm.engine.impl.core.instance.CoreExecution;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.spring.application.SpringServletProcessApplication;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaField;
import org.camunda.spin.plugin.variable.SpinValues;
import org.camunda.spin.plugin.variable.value.JsonValue;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

import jpl.cws.core.code.CodeService;
import jpl.cws.core.db.DbService;
import jpl.cws.core.log.CwsEmailerService;
import jpl.cws.core.log.CwsWorkerLoggerFactory;
import jpl.cws.core.service.ProcessService;

/**
 * Process Application exposing this application's resources the process engine. 
 */
@ProcessApplication
public class CwsEngineProcessApplication extends SpringServletProcessApplication implements InitializingBean {
	
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private CodeService cwsCodeService;
	@Autowired private RepositoryService repositoryService;
	@Autowired private DbService cwsDbService;
	@Autowired private ProcessService processService;
	@Autowired private EngineDbService engineDbService;
	@Autowired private WorkerService workerService;
	@Autowired private IdentityService identityService;
	@Autowired private WorkerDaemon workerDaemon;
	@Autowired private WorkerHeartbeatDaemon workerHeartbeatDaemon;
	@Autowired private WorkerExternalTaskLockDaemon workerExternalTaskLockDaemon;
	@Autowired private CwsEmailerService cwsEmailerService;

	@Autowired private JmsTemplate jmsProcessExternalTasksTemplate;

	//@Autowired private ExternalTaskService externalTaskService;
	
	@Value("${camunda.executor.service.max.pool.size}") private Integer EXEC_SERVICE_MAX_POOL_SIZE;
	@Value("${cws.admin.email}") private String cwsAdminEmail;
	@Value("${cws.smtp.hostname}") private String cwsSMTPHostname;
	@Value("${cws.smtp.port}") private String cwsSMTPPort;
	@Value("${send.user.task.assignment.emails}") private String sendUserTaskAssignmentEmails;
	@Value("${user.task.assignment.subject}") private String taskAssignmentSubject;
	@Value("${user.task.assignment.body}") private String taskAssignmentBody;
	@Value("${cws.engine.jobexecutor.enabled}") private boolean isJobExecutorEnabled;
	@Value("${cws.worker.id}") private String workerId;
	@Value("${startup.autoregister.proces.defs}") private boolean startupAutoregisterProcessDefs;
	
	private Logger log;
	
	/**
	 * In a @PostDeploy Hook you can interact with the process engine and access 
	 * the processes the application has deployed. 
	 */
	@PostDeploy
	public void onDeploymentFinished(ProcessEngine processEngine) {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		workerService.setProcAppRef(this.getReference());
		
		System.out.println("**************************************************");
		System.out.println("******  CWS ENGINE PROCESS APP STARTING...  ******");
		System.out.println("**************************************************");
		
		initCwsTables();
		
		// Update to the latest snippets code
		try {
			cwsCodeService.updateToLatestCode();
		} catch (Exception e) {
			log.error("Problem updating the \"cws\" bean to the latest snippets code", e);
		}
		
		workerService.setJobExecutorMaxPoolSize(EXEC_SERVICE_MAX_POOL_SIZE, true);
		
		workerService.initProcessCountersAndLimits();
		
		
		log.info("isJobExecutorEnabled = " + isJobExecutorEnabled);
		
		// FIXME: If job executor not enabled, then disable via JMX??
		
		try {
			engineDbService.initCwsTables();

			String lockOwner = ((ProcessEngineImpl)processEngine).getProcessEngineConfiguration().getJobExecutor().getLockOwner();
			
			engineDbService.createOrUpdateWorkerRow(lockOwner);
		} catch (SQLException e) {
			log.error("Problem initializing cws-engine database tables", e);
		}
		
		// Immediately update process counters and limits for this worker
		//
		workerService.updateProcessCountersAndLimits();
		
		// Update set of accepting proc def keys (memory map) for this worker
		//
		log.debug("Updating set of accepting proc def keys for worker...");
		workerService.updateAcceptingProcDefKeys();
		
		
		// Immediately get deployments available for execution by THIS worker, and
		// register them with this process application.
		//
		workerService.updateProcessAppDeploymentRegistrations(getReference());
		
		
		// Start the WorkerDaemon, which runs background operations
		// related to this worker.
		//
		workerDaemon.setProcessApplication(getReference());
		workerDaemon.start();
		workerHeartbeatDaemon.start();
		workerExternalTaskLockDaemon.start();
		
		// Update database with initial heart beat, so others will know we are alive.
		// This also set the worker's status to "up".
		//
		workerService.heartbeat();
		
		// If this worker is in the startupAutoregisterProcessDefs mode
		//  (e.g. used for auto-scaling of Workers in the cloud)
		// then auto-register all proc defs at this time
		//
		if (startupAutoregisterProcessDefs) {
			workerService.autoRegisterAllProcDefs();
		}
		
		System.out.println("**************************************************");
		System.out.println("******  CWS ENGINE PROCESS APP STARTED      ******");
		System.out.println("**************************************************");
	}
	
	
	private void initCwsTables() {
		try {
			cwsDbService.initCwsTables();
		} catch (SQLException e) {
			log.error("Problem while initializing CWS tables", e);
		}
	}
	
	
	/**
	 * Custom TaskListener callback implementation.
	 * 
	 */
	public TaskListener getTaskListener() {
		return new TaskListener() {
			
			@Override
			public void notify(DelegateTask delegateTask) {
				String eventName = delegateTask.getEventName();
				log.debug("TaskListener detected event : " + eventName);
				
				if (sendUserTaskAssignmentEmails.equalsIgnoreCase("Y")) {
					// If event was assignment, then send an assignment email
					if (eventName != null && eventName.equals("assignment")) {
						String assignee = delegateTask.getAssignee();
						
						if (assignee == null) {
							// Must be un-claiming a User task
							log.debug("assignee was null, so User task must be un-claiming.");
						}
						else {
							log.debug("Querying identity service for assignee: '" + assignee + "'...");
							// 
							User user = identityService.createUserQuery().userId(delegateTask.getAssignee()).singleResult();
							
							if (user != null) {
								// Get email address from User object
								String recipient = user.getEmail();
								
								log.info("Emailing '" + recipient + "' to let them know they have been assigned a User task...");
								emailUserTaskAssignment(user.getEmail(), user.getFirstName(), user.getLastName(), delegateTask.getName(), recipient);
							}
							else {
								log.error("User not found via Identity service for assignee: " + delegateTask.getAssignee());
							}
						}
					}
				}
			}
		};
	}
	
	private Collection<CamundaField> getFields(DelegateExecution execution) {
		
		try {
			return execution.getBpmnModelElementInstance().getExtensionElements().getElementsQuery().filterByType(CamundaField.class).list();
		}
		catch (Exception e) {
			return null;
		}
	}

	private CamundaField findField(String name, Collection<CamundaField> fields) {
		
		if (fields != null) {
			for (CamundaField field : fields) {
			
				if (field.getCamundaName().equals(name))
					return field;
			}
		}
		
		return null;
	}
	
	private String getFieldValue(String name, Collection<CamundaField> fields, ExpressionManager expressionManager, DelegateExecution execution) {
		
		CamundaField field = findField(name, fields);

		if (field != null) {
			
			String value = field.getCamundaExpression();
			
			if (value == null && field.getCamundaExpressionChild() != null) {
				value = field.getCamundaExpressionChild().getTextContent();
			}
			
			if (value != null) {
				Expression expression = expressionManager.createExpression(value);
				
				Object evalValue = expression.getValue(execution);
				
				return String.valueOf(evalValue);
			}
		}
		
		return null;
	}
	
	
	/**
	 * Custom ExecutionListener callback implementation
	 * 
	 */
	public ExecutionListener getExecutionListener() {
		return new ExecutionListener() {
			
			@Override
			public void notify(final DelegateExecution execution) throws Exception {
				
				// A Camunda bug most likely exists for "multi" tasks, where some of the below information
				// can be null.  Therefore we need to check for null, and abort, if null
				if (execution == null) {
					log.warn("execution was null inside of ExecutionListener.notify!");
					return; // abort
				}
				if (execution.getBpmnModelElementInstance() == null) {
					log.warn("execution.getBpmnModelElementInstance() was null inside of ExecutionListener.notify! " +
							"(eventName=" + execution.getEventName() + ")");
					return; // abort since we can't really do anything...
				}
				String elementName = execution.getBpmnModelElementInstance().getClass().getSimpleName();
				
				// Log all variables, if TRACE is enabled
				//
				if (log.isTraceEnabled()) {
					Map<String,Object> variables = execution.getVariables();
					for (String varKey : variables.keySet()) {
						log.trace("ExecutionListener::notify fired : " + execution.getEventName() +
							" : " + execution.getCurrentActivityId() + " (" + elementName + ") " +
							"VARIABLE [" + varKey + " = " + variables.get(varKey) + "]");
					}
				}
				
				// If this is an event in a subprocess, then this doesn't count.
				// We only want to count events of the parent process.
				//
				String parentActivityInstanceId = execution.getParentActivityInstanceId();
				if (!parentActivityInstanceId.startsWith("SubProcess")) {
					
					// -------------------------
					// END EVENT DETECTED
					// -------------------------
					if (elementName.equals("EndEventImpl")) {
						// FIXME: Make sure this triggers for every end event
						
						try {
							final CoreExecutionContext<? extends CoreExecution> executionContext = Context.getCoreExecutionContext();
							String eventSource = executionContext.getExecution().getEventSource().toString();
							if (execution.getEventName().equals("end") && eventSource.startsWith("Activity")) {
								log.trace("END EVENT DETECTED!!!!!!!!");
								String procDefKey = getProcDefKey(execution);
								if (procDefKey == null) {
									log.error("procDefKey unable to be determined!");
								}
								log.trace("sendProcEventTopicMessageWithRetries");

								String startedOnWorkerId = execution.getVariable("startedOnWorkerId").toString();
								if (startedOnWorkerId != null && !startedOnWorkerId.equals(workerId)) {
									log.debug("PROCESS '" + procDefKey + "' - Started on worker[" + startedOnWorkerId + "] != ended[" + workerId + "] on worker");
								}
								// FIXME: UUID might not be right
								if (execution.getVariable("uuid") != null) {
									processService.sendProcEventTopicMessageWithRetries(
										execution.getProcessInstanceId(),
										execution.getVariable("uuid").toString(),
										procDefKey,
										repositoryService.getProcessDefinition(execution.getProcessDefinitionId()).getDeploymentId(),
										"processEndEventDetected");
								} else {
									log.warn("WARNING: UUID of an instance of procDef '" + procDefKey + "' cannot be found. ");
								}
							}
						}
						catch (Throwable t) {
							log.error("Unexpected Throwable", t);
							throw t;
						}
					}
					
					
					// TODO: put any other execution listener hooks in here
					
				} // end check for non-sub-process event
				
				if (elementName.equals("ServiceTaskImpl")) {
					
					try {
						final CoreExecutionContext<? extends CoreExecution> executionContext = Context.getCoreExecutionContext();
						String eventSource = executionContext.getExecution().getEventSource().toString();
						if (execution.getEventName().equals("start") && eventSource.startsWith("Activity")) {
							
							// TODO: Check if this is an external task, otherwise not needed
							//String topicAttrib = execution.getBpmnModelElementInstance().getAttributeValue("camunda:topic");
							
							Collection<CamundaField> fields = getFields(execution);
							
							// timeout is only specified in the new cmd line external tasks, so if it's not found then it is 
							// likely an older model cmdLine task running or another service task type
							
							if (findField("timeout", fields) != null) {

								final String activityId = execution.getCurrentActivityId();
								
								String fieldName = null;

								CmdLineInputFields cmdFields = new CmdLineInputFields();

								try {
									log.debug("Service Task with External Task's ExecutionId = " + execution.getId());
									log.debug("Service task current ActivityId = " + execution.getCurrentActivityId());
									log.debug("EventSource = " + eventSource);
									
									// FIXME:  Can expressionManager be constructed once, outside of this method?
									//         If so, it would be faster, and it wouldn't have to be
									//         passed to each getFieldValue method below...
									ExpressionManager expressionManager = Context
										.getProcessEngineConfiguration()
										.getExpressionManager();

									fieldName = "cmdLine";
									cmdFields.command = getFieldValue(fieldName, fields, expressionManager, execution);
									
									fieldName = "workingDir";
									cmdFields.workingDir = getFieldValue(fieldName, fields, expressionManager, execution);
									
									if (cmdFields.workingDir == null) {
										cmdFields.workingDir = System.getProperty("user.home");
									}

									fieldName = "successExitValues";
									cmdFields.successfulValues = getFieldValue(fieldName, fields, expressionManager, execution).replaceAll("\\s+", "");

									// Verify field format
									for (String successCode : cmdFields.successfulValues.split(",")) {
										Integer.parseInt(successCode);
									}

									fieldName = "exitCodeEvents";
									cmdFields.exitCodeEvents = getFieldValue(fieldName, fields, expressionManager, execution).replaceAll("\\s+", "");

									// Verify field format
									String[] exitCodeMapArray = cmdFields.exitCodeEvents.split(",");
									for (String exitCodeMap : exitCodeMapArray) {

										String[] keyVal = exitCodeMap.split("=");
										if (keyVal.length != 2) {
											throw new Exception("Bad format: " + exitCodeMap);
										}
										Integer.parseInt(keyVal[0]);
									}

									fieldName = "throwOnFailures";
									cmdFields.throwOnFailures = Boolean.parseBoolean(getFieldValue(fieldName, fields, expressionManager, execution));
									
									fieldName = "throwOnTruncatedVariable";
									cmdFields.throwOnTruncatedVariable = Boolean.parseBoolean(getFieldValue(fieldName, fields, expressionManager, execution));
									
									fieldName = "timeout";
									cmdFields.timeout = Long.parseLong(getFieldValue(fieldName, fields, expressionManager, execution));
									
									fieldName = "retries";
									cmdFields.retries = Integer.parseInt(getFieldValue(fieldName, fields, expressionManager, execution));
									
									fieldName = "retryDelay";
									cmdFields.retryDelay = Integer.parseInt(getFieldValue(fieldName, fields, expressionManager, execution));

									JsonValue jsonValue = SpinValues.jsonValue(new Gson().toJson(cmdFields)).create();
									execution.setVariable(activityId + "_in", jsonValue);
								}
								catch (Throwable t) {
									log.error("Error parsing cmdLine fields", t);

									String msg = "Error parsing field '" + fieldName + "': " + t.getMessage();

									execution.setVariable(activityId + "_errorMsg", msg);
								}
								finally {
									
									// Tell worker to process external tasks immediately
									//
									try {
										// Send message to processExternalTasksTopic, requesting an immediate fetch of more tasks.
										// The listener acts as a debouncer and only processes this message if
										// the external task service is not already processing
										jmsProcessExternalTasksTemplate.send(Session::createBytesMessage);
									} catch (Throwable e) {
										cwsEmailerService.sendNotificationEmails("CWS JMS Error",
												"Severe Error!\n\nCould not access AMQ.\n\nDetails: " + e.getMessage());
										log.error("Could not access AMQ.", e);
									}
								}
							}
						}
					}
					catch (Throwable t) {
						log.error("Unexpected Throwable", t);
						throw t;
					}
					
				}
			}
		};
	}

	/**
	 * Helper method to get procDefKey from execution object, using multiple methods.
	 */
	private String getProcDefKey(DelegateExecution execution) {
		BpmnModelInstance bpmnModelInstance = execution.getBpmnModelInstance();
		String procDefKey = 
				bpmnModelInstance.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class)
				.iterator().next().getAttributeValue("id");
		if (procDefKey == null) {
			// See: https://groups.google.com/forum/#!topic/camunda-bpm-users/anXc5jwV6nA
			log.warn("procDefKey not determined from model! Trying second approach...");
			procDefKey = repositoryService.getProcessDefinition(execution.getProcessDefinitionId()).getKey();
		}
		return procDefKey;
	}
	
	
	/**
	 * Sends a User task assignment notification email to the specified recipients.
	 * 
	 */
	private void emailUserTaskAssignment(
			String userEmail,
			String userFirstName,
			String userLastName,
			String taskName, String ... recipients) {
		try {
			Email email = new HtmlEmail();
			
			// Perform any text substitutions on subject / body
			//
			log.trace("pre subject: "+taskAssignmentSubject);
			log.trace("pre body: "+taskAssignmentBody);
			String emailSubject = taskAssignmentSubject.toString();
			emailSubject = emailSubject.replaceAll("CWS_TASK_NAME", taskName == null ? "" : taskName);
			emailSubject = emailSubject.replaceAll("CWS_USER_EMAIL", userEmail == null ? "" : userEmail);
			emailSubject = emailSubject.replaceAll("CWS_USER_FIRSTNAME", userFirstName == null ? "" : userFirstName);
			emailSubject = emailSubject.replaceAll("CWS_USER_LASTNAME", userLastName == null ? "" : userLastName);
			String emailBody = taskAssignmentBody.toString();
			emailBody = emailBody.replaceAll("CWS_TASK_NAME", taskName == null ? "" : taskName);
			emailBody = emailBody.replaceAll("CWS_USER_EMAIL", userEmail == null ? "" : userEmail);
			emailBody = emailBody.replaceAll("CWS_USER_FIRSTNAME", userFirstName == null ? "" : userFirstName);
			emailBody = emailBody.replaceAll("CWS_USER_LASTNAME", userLastName == null ? "" : userLastName);
			
			
			// set the body as an HTML message
			((HtmlEmail) email).setHtmlMsg("<html>"
					+ emailBody.replaceAll("\\n", "<br/>").replaceAll("&#10;", "<br/>") + "</html>");
			// set the alternative text message
			((HtmlEmail) email).setTextMsg("TEXT ALTERNATIVE: " + emailBody.replaceAll("&#10;", "\n"));
			
			email.setHostName(cwsSMTPHostname);
			email.setSmtpPort(Integer.parseInt(cwsSMTPPort));
			email.setFrom(cwsAdminEmail);
			email.setSubject(emailSubject);
			
			for (String recip : recipients) {
				email.addTo(recip.trim());
				log.debug("About to send email to " + recip + "...");
			}

			log.debug("  FROM   : " + cwsAdminEmail);
			log.debug("  SUBJECT: " + emailSubject);
			log.debug("  BODY   : " + emailBody);
			
			// Send email
			//
			email.send();
			
		} catch (Throwable e) {
			log.error("Problem occurred while sending email", e);
			// FIXME: throw?
		}
	}
	
	
	@PreUndeploy
	public void unregisterProcessApplication(ProcessEngine processEngine) {
		System.out.println("unregisterProcessApplication("+processEngine+")");
		System.out.println("**************************************************");
		System.out.println("******  CWS ENGINE PROCESS APP STOPPING...  ******");
		System.out.println("**************************************************");
		
		log.warn("  Interrupting workerDaemon bean...");
		workerDaemon.interrupt();
		workerHeartbeatDaemon.interrupt();
		workerExternalTaskLockDaemon.interrupt();
		workerService.bringWorkerDown();
		
		
		// TODO: investigate what to undeploy here..
		// SEE: https://groups.google.com/forum/#!topic/camunda-bpm-users/eTK_cPvoneE
	}

	@Override
	public void afterPropertiesSet() throws Exception {
        System.out.println(this + " afterPropertiesSet() : cwsWorkerLoggerFactory = " + cwsWorkerLoggerFactory);
    }
}

