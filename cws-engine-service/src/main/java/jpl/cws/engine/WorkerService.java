package jpl.cws.engine;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.camunda.bpm.application.ProcessApplicationReference;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;
import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.log.CwsWorkerLoggerFactory;
import jpl.cws.core.service.SpringApplicationContext;
import jpl.cws.engine.listener.ProcessStartReqProcessor;

/**
 * Service class for worker.
 * 
 * @author ghollins
 *
 */
public class WorkerService implements InitializingBean {
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	@Autowired private EngineDbService engineDbService;
	@Autowired private SchedulerDbService schedulerDbService;
	@Autowired private RuntimeService runtimeService;
	@Autowired private RepositoryService repositoryService;
	@Autowired private ManagementService managementService;
	
	private ProcessApplicationReference procAppRef; // need this like this, because of chicken-before-egg situation with CwsEngineProcessApplication class
	
	@Value("${cws.jmx.service.url}") private String JMX_SERVICE_URL;
	@Value("${cws.worker.id}") private String workerId;
	
	// Camunda Job Executor max pool size (and core pool size -- they should be the same)
	@Value("${camunda.executor.service.max.pool.size}") private Integer EXEC_SERVICE_MAX_POOL_SIZE;
	
	@Value("${cws.engine.jobexecutor.enabled}") private boolean jobExecutorEnabled;

	@Value("${cws.tomcat.lib}") private String cwsTomcatLib;

	@Value("${worker.max.num.running.procs}") private int workerMaxNumRunningProcs;

	private Logger log;
	
	public static AtomicInteger processorExecuteCount = new AtomicInteger(0);
	
	static Object procStateLock = new Object();
	
	// Map of procDefKey and count of active process instances
	public static Map<String,Integer> processCounters = new HashMap<String,Integer>();

	private static Map<String,Integer> workerMaxProcInstances = new HashMap<String,Integer>();

	private static Set<String> procStartReqUuidStartedThisWorker = new HashSet<String>();
	private static Set<String> acceptingProcDefKeys = new HashSet<String>();
	//private static Set<String> runningToCompleteTransitionUuids = new HashSet<String>();
	
	// FIXME: make this number configurable or determine dynamically
	public static final int PROCESSOR_THREAD_POOL_SIZE = 2;
	
	// FIXME: make this number configurable or determine dynamically
	public static final int STARTER_THREAD_POOL_SIZE   = 2;
	
	public static String lastProcCounterStatusMsg;
	
	final ThreadFactory processorThreadFactory = new ThreadFactoryBuilder()
		.setNameFormat("procReq-Processor-%d")
		.setDaemon(true)
		.build();
	private ExecutorService processorPool;
	
	final ThreadFactory starterThreadFactory = new ThreadFactoryBuilder()
		.setNameFormat("procReq-Starter-%d")
		.setDaemon(true)
		.build();
	private ExecutorService starterPool;
	
	private static long skippedMessages = 0;
	
	private JMXServiceURL url;
	JMXConnector jmxc;
	MBeanServerConnection mbsc;
	ObjectName serviceName;
	
	public WorkerService() {
		System.out.println("WorkerService constructor...");
	}
	
	public void setProcAppRef(ProcessApplicationReference procAppRef) {
		this.procAppRef = procAppRef;
	}
	
	
	public boolean enabledForProcDefKey(String procDefKey) {
		
		if (acceptingProcDefKeys.contains(procDefKey) && 
			workerMaxProcInstances.containsKey(procDefKey) && 
			workerMaxProcInstances.get(procDefKey) > 0) {
			return true;
		}
		
		return false;
	}

	/**
	 * Get list of proc_def_keys currently enabled for this worker.
	 * Used when fetching external tasks
	 * @return List of currently active proc_def_keys
	 */
	public List<String> getEnabledProcDefKeys() {
		ArrayList<String> ret = new ArrayList<>();
		for (String proc_def_key : acceptingProcDefKeys) {
			if (enabledForProcDefKey(proc_def_key)) {
				ret.add(proc_def_key);
			}
		}

		return ret;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("WorkerService afterPropertiesSet...");
		processorPool = Executors.newFixedThreadPool(PROCESSOR_THREAD_POOL_SIZE, processorThreadFactory);
		starterPool   = Executors.newFixedThreadPool(STARTER_THREAD_POOL_SIZE,   starterThreadFactory);
		
		refreshJmxConnector();
	}
	
	/**
	 * Upon startup of a Worker, we want to update the counters.
	 */
	public void initProcessCountersAndLimits() {
		// Create initial data structures from DB
		//  (with proc counts set to zero)
		updateProcessCountersAndLimits();
		
		// Fill in the counts from the DB state
		//
		List<Map<String,Object>> rows = schedulerDbService.getIncompleteProcessInstancesForWorker(workerId);
		for (Map<String,Object> row : rows) {
			//String key = row.get("proc_def_key").toString();
			String procDefKey = row.get("proc_def_key").toString();
			int count = Integer.parseInt(row.get("cnt").toString());
			
			synchronized (procStateLock) { //procCountsLock
				processCounters.put(procDefKey, count);
			}
			
		}
		
		log.info("AFTER INIT: limits: " + workerMaxProcInstances + ",  counts: " + processCounters);
	}
	

	public void updateStats() {
		
		File[] logs = getLogFiles();

		try {
			long freeSpaceBytes = getDiskFreeSpace();
			
			engineDbService.updateStats(logs, freeSpaceBytes);
			
		} catch (Exception e) {
			
			log.error("Error updateStats: " + e.getMessage());
		}
	}
	
	private File[] getLogFiles() {
		
		File dir = new File(cwsTomcatLib + "/../logs");
		
		File[] files = dir.listFiles(new FilenameFilter() {
			
			//apply a filter
			@Override
			public boolean accept(File dir, String name) {
				
				if (name.startsWith("catalina") || name.startsWith("localhost")) {
					return true;
				}
				
				return false;
			}
		});
		
		return files;
	}
	
	
	private long getDiskFreeSpace() throws Exception {
		
		FileStore store = Files.getFileStore(Paths.get(cwsTomcatLib));
		
		return store.getUsableSpace();
	}
	
	
	public void cleanupLogs(Date keepDate) {
		
		File[] logs = getLogFiles();
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);

		for (File logFile : logs) {
			
			// e.g. cws.2018-10-29.log, localhost_access_log.2018-11-01.txt
			String filename = logFile.getName();
			
			try {
				String[] filenameSplit = filename.split("\\.");
				
				if (filenameSplit.length == 3) {
					
					Date date = formatter.parse(filenameSplit[1]);
					
					log.debug("Found log file '" + filename + "' with date: " + format.format(date));
					
					if (date.before(keepDate)) {
						
						logFile.delete();
						
						log.debug("Deleted expired log file: " + filename);
					}
				}
				else {
					log.debug("Skipped log file: " + filename);
				}
			}
			catch (ParseException e) {
				
				log.error("Could not parse date in log: " + filename, e);
			}
			catch (Exception e) {
				
				log.error("Error while processing log: " + filename, e);
			}
		}
	}
	
	/**
	 * Updates knowledge from DB
	 * 
	 */
	public boolean updateProcessCountersAndLimits() {
		log.trace("updateProcessCountersAndLimits for workerId = " + workerId);
		
		List<Map<String,Object>> rows = schedulerDbService.getWorkerProcDefRows(workerId, null);
		
		// Update max limits,
		// and set initial values for process counters, if a new process definition was deployed
		//
		synchronized (procStateLock) { // procCountsLock
			workerMaxProcInstances.clear();
			for (Map<String,Object> row : rows) {
				String procDefKey = row.get("proc_def_key").toString();
				
				workerMaxProcInstances.put(
						procDefKey,
						Integer.parseInt(row.get("max_instances").toString()));
				
				// only set counter value, if it's a new process definition
				if (processCounters.get(procDefKey) == null) {
					processCounters.put(procDefKey, 0);
				}
			}
		}
		
		// Sync counters from DB
		syncCounters("updateProcessCountersAndLimits");
		
		// Only log if configuration changed
		//
		String postConfig = "limits: " + workerMaxProcInstances + ",  counts: " + processCounters;
		if (lastProcCounterStatusMsg == null || !lastProcCounterStatusMsg.equals(postConfig)) {
			log.info("NEW: " + postConfig + ",  OLD: " + lastProcCounterStatusMsg);
			lastProcCounterStatusMsg = postConfig;
			return true; // config changed
		}
		
		return false; // no change to config
	}
	
	
	/**
	 * If no more process starts are being accepted for the specified deploymentId,
	 * then unregister the process application.
	 */
	public void unRegisterDeploymentIfAppropriate(String deploymentId) {
		Integer acceptingNew = schedulerDbService.isWorkerProcDefAcceptingNew(workerId, deploymentId);
		if (acceptingNew == null || acceptingNew == 0) { // no row, or not accepting new
			log.debug("calling managementService.unregisterProcessApplication("+deploymentId+", true)...");
			managementService.unregisterProcessApplication(deploymentId, true);
		}
	}
	
	
	/**
	 * 
	 */
	public int updateProcessAppDeploymentRegistrations(ProcessApplicationReference processApp) {
		log.trace("updateProcessAppDeploymentRegistrations()...");
		
		// Get deployments available for execution by all engines, and
		// register them with this process application.
		//
		// processEngine.getRepositoryService().createDeploymentQuery().deploymentName("available-to-all-engines").list();
		List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
		
		// Construct set of applicable deployment IDs
		//
		Set<String> applicableProcDefKeys = new HashSet<String>();
		for (Map<String,Object> row : schedulerDbService.getWorkerProcDefRows(workerId, true)) {
			applicableProcDefKeys.add(row.get("proc_def_key").toString());
		}
		
		if (log.isTraceEnabled()) {
			if (!applicableProcDefKeys.isEmpty()) {
				log.trace(applicableProcDefKeys.size() + " proc defs applicable to this worker.");
				for (String procDefKey : applicableProcDefKeys) {
					log.trace("applicable proc def key: " + procDefKey);
				}
			}
		}
		
		Set<String> alreadyRegisteredDeployments = managementService.getRegisteredDeployments();
		for (String regDeployment : alreadyRegisteredDeployments) {
			log.trace("already registered: " + regDeployment);
		}
		
		int numChanges = 0;
		int numNewDeployments = 0;
		
		for (Deployment deployment : deployments) {
			String deploymentId = deployment.getId();
			log.trace("got deployment: " + deploymentId);
			
			List<ProcessDefinition> procDefs = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).list();
			
			boolean doDeploy = false;
			for (ProcessDefinition procDef : procDefs) {
				if (applicableProcDefKeys.contains(procDef.getKey())) {
					doDeploy = true;
					break;
				}
			}
			
			if (doDeploy) {
				
				// Only register, if not already registered
				//
				if (!alreadyRegisteredDeployments.contains(deploymentId)) {
					log.debug("REGISTERING DEPLOYMENT: " + deployment);
					
					// register the deployment
					managementService.registerProcessApplication(deploymentId, processApp);
					
					numChanges++;
					numNewDeployments++;
				}
				else {
					log.trace("SKIPPING REGISTRATION OF : " + deployment);
				}
				
				// make sure there are cws_worker_proc_def rows for each process definition
				//
				for (ProcessDefinition procDef : procDefs) {
					String procDefKey = procDef.getKey();
					engineDbService.createIfNotExistsNodeProcess(
						workerId, procDefKey, deploymentId,
						SchedulerDbService.DEFAULT_WORKER_PROC_DEF_MAX_INSTANCES
					);
				}
				
			}
			else {
				//
				// NOT an applicable deployment for this worker,
				// so unregister the deployment...
				//
				synchronized (procStateLock) { // procCountsLock
					// Only unregister, if it's currently registered
					//
					if (alreadyRegisteredDeployments.contains(deploymentId)) {
						
						String procDefKey = null;
						for (ProcessDefinition procDef : procDefs) {
							procDefKey = procDef.getKey();
							log.debug("procDefKey = " + procDefKey);
							// FIXME: can we not loop here?
						}
						log.debug("processCounters = " + processCounters + ", procDefKey = " + procDefKey);
						Integer curCount = processCounters.get(procDefKey);
						if (curCount == null || curCount == 0) {
							log.debug("un-register '" + deploymentId + "' due to change in deployment status (curCount = " + curCount + ")");
							// Un-register the process deployment if appropriate
							// (if accepting_new is false)
							//
							unRegisterDeploymentIfAppropriate(deploymentId);
						}
						else {
							log.warn("NOT CALLING unRegisterDeploymentIfAppropriate.  Even though proc def '" +
									procDefKey + "' no longer applicable for this worker, there are still " + curCount +
									" processes running: " + processCounters);
						}
					}
				}
			}
		}
		
		// Log final state of registrations, if there were changes
		//
		if (numChanges > 0) {
			log.info("deployments:  " + managementService.getRegisteredDeployments());
		}
		
		return numNewDeployments;
	}
	
	
	/**
	 * Gets the 'job_executor_max_pool_size' value out of the database,
	 * and updates the job executor via JMX.
	 * 
	 */
	public void updateJobExecutorMaxPoolSize() {
		int jobExecutorMaxPoolSize = schedulerDbService.getWorkerJobExecutorMaxPoolSize(workerId);
		setJobExecutorMaxPoolSize(jobExecutorMaxPoolSize, false);
	}
	
	private void refreshJmxConnector() {
		try {
			url = new JMXServiceURL(JMX_SERVICE_URL);
			// Get connector to JMX
			jmxc = JMXConnectorFactory.connect(url, null);
			jmxc.connect();
			
			// Get MBean server connection
			mbsc = jmxc.getMBeanServerConnection();
			
			// Set the service name (executor-service endpoint name)
			serviceName = new ObjectName("org.camunda.bpm.platform:type=executor-service");
			
		}
		catch (Exception e) {
			log.error("problem refreshing JMX connector", e);
		}
	}
	
	public void heartbeat() {
		log.trace("workerHeartbeat...");
		try {
			refreshJmxConnector();
			Integer activeCount = (Integer)mbsc.getAttribute(serviceName, "ActiveCount");
			mbsc = null;
			if (activeCount == null) {
				log.error("activeCount returned via JMX was null!");
				engineDbService.workerHeartbeat(0);
			}
			else {
				engineDbService.workerHeartbeat(activeCount.intValue());
			}
			activeCount = null;
		}
		catch (Exception e) {
			log.error("problem encountered during heartbeat()", e);
		}
		finally {
			if (jmxc != null) {
				try {
					jmxc.close();
					jmxc = null;
				} catch (IOException e) {
					log.error("problem closing jmxc", e);
				}
			}
			
			url = null;
			serviceName = null;
		}
	}
	
	
	public void setWorkerAcceptingNew(boolean acceptingNew) {
		log.info("Setting worker '" + workerId + "' acceptingNew to " + acceptingNew + "...");
		schedulerDbService.setWorkerAcceptingNew(acceptingNew, workerId);
	}
	
	
	public void setWorkerStatus(String newStatus) {
		log.info("Setting worker '" + workerId + "' status to '" + newStatus + "'...");
		schedulerDbService.updateWorkerStatus(workerId, newStatus);
	}
	
	
	public void autoRegisterAllProcDefs() {
		try {
			// Get deployments available for execution by all engines,
			// and register them for this worker
			//
			List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
			for (Deployment deployment : deployments) {
				String deploymentId = deployment.getId();
				log.info("AUTO-REGISTER DEPLOYMENT: " + deployment.getName() + " " + deploymentId);
				List<ProcessDefinition> procDefs = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).active().list();
				
				for (ProcessDefinition procDef : procDefs) {
					log.info("  PROCDEF = " + procDef.getKey());
					schedulerDbService.updateWorkerProcDefEnabled(workerId, procDef.getKey(), deploymentId, true);
				}
			}
			
			// Now that the database is updated, perform the config change actions.
			// This will actually register the deployments, update in the in-memory counters, etc..
			//
			doWorkerConfigChangeActions();
		}
		catch (Throwable t) {
			log.error("Problem encountered during process of auto-registering all proc defs", t);
		}
	}
	
	
	/**
	 * Gets called when a worker config change message arrives,
	 * or after a Worker in "startup_autoregister_process_defs" mode starts up, and adds new proc def configuration
	 * 
	 */
	public void doWorkerConfigChangeActions() {
		// Check for any new deployments to register
		//
		int numNewDeployments = updateProcessAppDeploymentRegistrations(procAppRef);
		
		boolean countersOrLimitsChanged = updateProcessCountersAndLimits();
		
		log.debug("Updating set of accepting proc def keys for worker...");
		updateAcceptingProcDefKeys();
		
		updateJobExecutorMaxPoolSize();
		
		// If we have new deployments, or limits have changed,
		// then there is a potential to run new processes
		//
		if (numNewDeployments > 0 || countersOrLimitsChanged) {
			log.debug("deployments (+" + numNewDeployments + ") or proc limits changed, so triggering procStartReqAction()...");
			procStartReqAction(null, "worker config changed");
		}
	}
	
	
	/**
	 * 
	 */
	public void procStartReqAction(String limitToProcDefKey, String reason) {
		if (!jobExecutorEnabled) {
			// For installations (such as the console-only), where processes aren't
			// intended to be initiated, except manually, then don't look for more
			// processes to execute.
			return;
		}
		
		int count = processorExecuteCount.incrementAndGet();
		//log.debug("processorExecuteCount = " + count);
		
		// FIXME: remove this check once confident
		if (count <= 0) { 
			log.error("unexpected counter value! " + count);
		}
		
		// If we have already maximized the thread pool,
		// then don't unnecessarily submit more jobs to it.
		if (count > PROCESSOR_THREAD_POOL_SIZE) {
			if (skippedMessages > (Long.MAX_VALUE-1)) {
				log.warn("rolling over skippedMessages variable...");
				skippedMessages = 0;
			}
			if (++skippedMessages % 50 == 0) {
				log.warn("skipped " + skippedMessages + " messages (procStartReqAction) so far (processor count = " + count + ").");
			}
			count = processorExecuteCount.decrementAndGet(); // reset to where it was
			
			log.debug("NOT PERFORMING START REQUEST ACTION (current processorExecuteCount = " + count + ", reason = '" + reason + "')");
			
			// FIXME: remove this check once confident
			if (count < 0) { 
				log.error("unexpected counter value! " + count);
			}
		}
		else {
			processorPool.execute(
				new ProcessStartReqProcessor(starterPool, repositoryService, runtimeService, schedulerDbService, this, log, limitToProcDefKey)
			);
		}
	}
	
	
	public void updateAcceptingProcDefKeys() {
		// Construct set of applicable deployment IDs
		//
		List<Map<String,Object>> rows = schedulerDbService.getWorkerProcDefRows(workerId, true);
		synchronized (procStateLock) { // procCountsLock
			acceptingProcDefKeys.clear();
			for (Map<String,Object> row : rows) {
				acceptingProcDefKeys.add(row.get("proc_def_key").toString());
			}
		}
	}
	
	
	/**
	 * This method figures out how much capacity this worker has to run extra processes,
	 * then claims that many processes in the database.
	 * 
	 * If processes were successfully claimed, then the in-memory process counter is
	 * incremented by the amount claimed, and the claimed rows are returned.
	 * 
	 */
	public List<Map<String,Object>> claimWithCounter(String limitToProcDefKey) {
		long t0 = System.currentTimeMillis();
		
		List<String> claimUuids = new ArrayList<String>();
		
		// For each defined process max (should be list of all eligible PDs),
		//  -- determine how many more can be run
		//  -- attempt to claim that many
		//  -- updated in-memory counters to reflect actual # claimed
		//
		//log.debug("workerMaxProcInstances: " + workerMaxProcInstances);
		
		long t1 = 0;
		
		synchronized (procStateLock) { // procCountsLock
			t1 = System.currentTimeMillis();

			int procSetSize = 0;
			//int totalCurrentRunningProcsOnWorker = 0;
			Map<String,Integer> currentCounts = new HashMap<String,Integer>();
			Map<String,Integer> remainders = new HashMap<String,Integer>();
			Map<String,Integer> queryLimitForProcSet = new HashMap<String,Integer>();
			Map<String,Integer> limitToProcDefKeyObject = new HashMap<String,Integer>();

			for (Entry<String,Integer> procMax : workerMaxProcInstances.entrySet()) {
				String procDefKey = procMax.getKey();

				if (limitToProcDefKey != null && !limitToProcDefKey.equals(procDefKey)) {
					continue;
				}
				int procMaxNumber = procMax.getValue();
				if (!acceptingProcDefKeys.contains(procDefKey)) {
					//log.debug("skipping " + procDefKey + " BECAUSE IT NOT ACCEPTING RIGHT NOW!!!!");
					continue;
				}

				currentCounts.put(procDefKey, processCounters.get(procDefKey));
				remainders.put(procDefKey, procMaxNumber - currentCounts.get(procDefKey));
				queryLimitForProcSet.put(procDefKey, Math.min(EXEC_SERVICE_MAX_POOL_SIZE, remainders.get(procDefKey)));

				//log.trace("getting currentCount for procDefKey " + procDefKey);
				//int currentCount = processCounters.get(procDefKey);
				//log.trace("currentCount for " + procDefKey + " is " + currentCount);
				//int remainder = procMaxNumber - currentCount;
				//log.trace("remainder for " + procDefKey + " is " + remainder);
				//int queryLimit = Math.min(EXEC_SERVICE_MAX_POOL_SIZE, remainder); // FIXME: needs revisit for proper min
				//log.trace("queryLimit for " + procDefKey + " is " + queryLimit);

			} // end for loop

			int totalCurrentRunningProcsOnWorker = 0;
			for (Entry<String,Integer> entry : processCounters.entrySet()) {
				totalCurrentRunningProcsOnWorker += entry.getValue().intValue();
			}

			// rename to workerMaxProcQueryLimit
			int MaxNumForProcsOnWorker = schedulerDbService.getMaxProcsValueForWorker(workerId);
			// this is for all procDefs cap
			int workerMaxProcQueryLimit = MaxNumForProcsOnWorker - totalCurrentRunningProcsOnWorker;

			int remaindersTotal = 0;
			for (int r: remainders.values()) {
				remaindersTotal += r;
			}

			if (remaindersTotal > 0 && workerMaxProcQueryLimit > 0) {
				// claim for remainder (marks DB rows as "claimedByWorker")

				int queryLimit = Math.min(MaxNumForProcsOnWorker, workerMaxProcQueryLimit);

				Map<String,List<String>> claimRowData =
					schedulerDbService.claimHighestPriorityStartReq(
						workerId, currentCounts, queryLimitForProcSet, queryLimit); // pass list of procDefkey and a map of queryLimit per procDefKey

				List<String> claimed = claimRowData.get("claimUuids");

				if (!claimed.isEmpty()) {
					// increment counter by amount that was actually claimed
					// in anticipation that the start will actually work.
					// If the start turns out not to later worker, then this count will be decremented at that time.
					//
					for (Map.Entry<String,Integer> procDefKey : processCounters.entrySet()) {
						int claimedInstCount = schedulerDbService.getCountForClaimedProcInstPerKey(procDefKey.getKey(), claimed);
						processCounters.put(procDefKey.getKey(), processCounters.get(procDefKey.getKey()) + claimedInstCount);
					}

					// update uuid list
					procStartReqUuidStartedThisWorker.addAll(claimRowData.get("claimedRowUuids"));
					//log.debug("procStartReqUuidStartedThisWorker = " + procStartReqUuidStartedThisWorker);

					log.debug("(CLAIMED " + claimed.size() + " / " + queryLimit + ", maxProcs=" + workerMaxProcInstances.entrySet() + ")  for procDefKeys '" + workerMaxProcInstances.keySet() + "' (limitToProcDefKey="+limitToProcDefKey+")" + ", workerMaxNumRunningProcs=" + MaxNumForProcsOnWorker);
					claimUuids.addAll(claimed);
				}
				//else {
				//	log.debug("NONE CLAIMED  (queryLimit=" + queryLimit + ", max=" + procMaxNumber + ")  for procDef '" + procDefKey + "' (limitToProcDefKey="+limitToProcDefKey+")");
				//}
			}
			else {
				log.debug("Remainder for Worker Max Process Limit [" + workerMaxProcQueryLimit + "] workerMaxProcQueryLimit <= 0 OR Total of remainders [" + remaindersTotal + "] is <=0, so not attempting claim. " +
					"(remainders = " + remainders +
					", procMaxNumbers = " + workerMaxProcInstances.entrySet() +
					", currentCounts = " + currentCounts + ")");
			}


			
		} // release lock
		
		// Timing logging
		//
		long preClaimTime = System.currentTimeMillis() - t0;
		if (preClaimTime > 50) {
			log.warn("pre-claim portion completed in " + preClaimTime + " ms. Wait for lock = " + (t1-t0));
		}
		else {
			log.trace("pre-claim portion completed in " + preClaimTime + " ms.");
		}
		
		if (!claimUuids.isEmpty()) {
			// If one or more claimed, then construct procStartReqData from DB data
			//
			return schedulerDbService.getClaimedProcInstRows(claimUuids);
		}
		else {
			return null;
		}
	}
	
	
	public boolean processEndedActions(String procDefKeyThatEnded, String uuidThatEnded) {
		log.trace("processEndedActions("+procDefKeyThatEnded+", " + uuidThatEnded + ")");
		synchronized (procStateLock) { // lock
			if (procStartReqUuidStartedThisWorker.contains(uuidThatEnded)) {
				//log.debug("DECREMENTING "+procDefKeyThatEnded);
				processCounters.put(procDefKeyThatEnded, processCounters.get(procDefKeyThatEnded) - 1);
				log.trace("PROCESS COUNTERS NOW AT : " + processCounters.get(procDefKeyThatEnded));
				procStartReqUuidStartedThisWorker.remove(uuidThatEnded);
				return true;
			}
			else {
				log.trace("Uuid that ended, not found in this worker's UUID list.");
				return false;
			}
		} // unlock
	}
	
	
	/*
	 * Synchronizes the processCounters map with the knowledge in Camunda's database.
	 * 
	 * Returns true, if the process counter state changed.
	 * 
	 */
	public boolean syncCounters(String reason) {
		if (!jobExecutorEnabled) {
			// For installations (such as the console-only), where processes aren't
			// intended to be initiated, except manually, then don't sync counters.
			return false;
		}
		
		if (log.isTraceEnabled()) {
			log.trace("------------ SYNC COUNTERS (" + reason + ")... ---------------");
		}
		
		boolean stateChanged = false;
		
		synchronized (procStateLock) { // lock
			
			if (log.isTraceEnabled()) {
				log.trace("PRE-SYNC COUNTERS: " + processCounters);
				log.trace("PRE-UUIDS        : " + procStartReqUuidStartedThisWorker.size());
			}
			
			// Get Camunda statuses for all process instances started by this worker.
			// [proc_def_key, status, cnt]
			// this actually gives you uuid, proc_def_key, status
			List<Map<String,Object>> statusesMap = 
					schedulerDbService.getStatsForScheduledProcs(procStartReqUuidStartedThisWorker);
			
			// Clear out the set of UUIDs -- we will re-populate below
			procStartReqUuidStartedThisWorker.clear();
			
			//
			// Get the set of processes that were
			// started by this worker, and are still running
			//
			Map<String,Integer> activeSet = new HashMap<String,Integer>();
			for (Map<String,Object> row : statusesMap) {
				String uuid = row.get("uuid").toString();
				String procDefKey = row.get("proc_def_key").toString();
				String rowStatus = row.get("status").toString();
				if (rowStatus.equals("running") || rowStatus.equals("claimedByWorker")) { //these both indicate running
					Integer cur = activeSet.get(procDefKey) == null ? 0 : activeSet.get(procDefKey);
					activeSet.put(procDefKey, cur + 1);
					procStartReqUuidStartedThisWorker.add(uuid);
				}
			}
			
			//
			// For processes that were started on this worker are still running,
			// modify counters to match current counts from database
			//
			for (String procDefKey : activeSet.keySet()) {
				Integer newVal = activeSet.get(procDefKey);
				Integer oldVal = processCounters.get(procDefKey);
				if (newVal != oldVal) {
					log.trace("COUNTER value changed: " + oldVal + " --> " + newVal);
					processCounters.put(procDefKey, newVal);
					stateChanged = true;
				}
			}
			
			//
			// Any processes that used to be in the processCounters map,
			// but are now gone, zero out...
			//
			for (String procDefKey : processCounters.keySet()) {
				if (!activeSet.containsKey(procDefKey)) {
					log.trace("ACTIVESET did not contain procDefKey '" + procDefKey + "', which was in old set");
					processCounters.put(procDefKey, 0); // zero out
					stateChanged = true;
				}
			}
			
			if (log.isTraceEnabled()) {
				log.trace("POST-SYNC COUNTERS: " + processCounters);
				log.trace("POST-UUIDS        : " + procStartReqUuidStartedThisWorker.size());
			}
		}	
		
		return stateChanged;
	}
	
	
	/**
	 * Via JMX, set attributes of JobExecutor
	 * 
	 */
	public void setJobExecutorMaxPoolSize(Integer executorServiceMaxPoolSize, boolean doDbUpdate) {
		if (executorServiceMaxPoolSize != null) {
			try {

				// Log information about JMX remote interface
				if (System.getProperty("com.sun.management.jmxremote") == null) {
					log.warn("JMX remote appears to be disabled");
					// FIXME: exit CWS app?
				} else {
					String portString = System.getProperty("com.sun.management.jmxremote.port");
					if (portString != null) {
						log.trace("JMX remote running on port " + Integer.parseInt(portString));
					}
				}
				
				// Get connector to JMX
				JMXServiceURL url = new JMXServiceURL(JMX_SERVICE_URL);
				JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
				jmxc.connect();
				
				// Get MBean server connection
				MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
				
				// Set the "MaximumPoolSize" attribute
				ObjectName serviceName = new ObjectName("org.camunda.bpm.platform:type=executor-service");
				
				// Set the CorePoolSize.
				// Through experimentation, we have seen that the max doesn't get reached, but
				// its the core size that counts.
				//
				Attribute attr2 = new Attribute("CorePoolSize", executorServiceMaxPoolSize);
				mbsc.setAttribute(serviceName, attr2);
				
				// Also set the MaximumPoolSize
				//
				// FIXME:  make this attribute configurable in "advanced" configuration of configure.sh
				Attribute attr = new Attribute("MaximumPoolSize", executorServiceMaxPoolSize);
				mbsc.setAttribute(serviceName, attr);
				
				// Finally set the member variable (used to determine how many to query in claim phase)
				//
				EXEC_SERVICE_MAX_POOL_SIZE = executorServiceMaxPoolSize;
				
				// Close JMX connector
				jmxc.close();
				
				log.info("Set Job Executor max pool size to " + executorServiceMaxPoolSize + " (JMX URL: " + JMX_SERVICE_URL + ")");
				
				if (doDbUpdate) {
					// Now update the DB, since setting was successful.
					//
					schedulerDbService.updateWorkerNumJobExecutorThreads(workerId, executorServiceMaxPoolSize);
				}
			}
			catch (Exception  e) { 
				log.error("Exception occurred while trying to set " +
						"org.camunda.bpm.platform:type=executor-service " +
						"MaximumPoolSize attribute to " + executorServiceMaxPoolSize +
						" (using JMX URL: " + JMX_SERVICE_URL+")", e);

				// FIXME: terminate server here?
			}
		}
	}
	
	
	/**
	 * Cleanup thing when bringing worker down.
	 * NOTE:  there are still cases where tomcat doesn't fully shutdown,
	 * I think due to threads hanging around.  A prime candidate seems to be the Camunda
	 * "pool-*" threads.  I tried interrupting them, but that doesn't seem to help..
	 * Further investigation is needed.
	 * 
	 */
	public void bringWorkerDown() {
		log.info("BRINGING WORKER DOWN...");
		
		//
		// DEBUGGING LOGGING
		// REMOVE ONCE ALL ISSUES WORKED OUT
		//
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Iterator<Thread> i = threadSet.iterator();
		int threadNum = 0;
		while(i.hasNext()) {
			Thread t = i.next();
			log.info("A THREAD(" + (++threadNum) + ") = " + t);
		}
		
		log.info("  Shutting down DefaultMessageListenerContainers...");
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
		
		CachingConnectionFactory cc = (CachingConnectionFactory)SpringApplicationContext.getBean("cachingConnectionFactory");
		cc.destroy();
		
		log.info("  Shutting down thread pools...");
		starterPool   = Executors.newFixedThreadPool(STARTER_THREAD_POOL_SIZE,   starterThreadFactory);
		try {
			log.info("   Shutting down starterPool...");
			starterPool.shutdown();
			log.info("   Shut down starterPool.");
			if (!starterPool.awaitTermination(20, TimeUnit.SECONDS)) {
				log.error("      starterPool timed out before terminating cleanly!");
			}
			else {
				log.info("      starterPool terminated cleanly (isTerminated = " + starterPool.isTerminated() + ", isShutdown = " + starterPool.isShutdown() + ")");
				List<Runnable> notRunJobs = starterPool.shutdownNow();
				log.info("      didn't run " + notRunJobs.size());
				starterPool = null;
			}
			
			
			log.info("    Shutting down processorPool...");
			processorPool.shutdown();
			log.info("    Shut down processorPool.");
			if (!processorPool.awaitTermination(20, TimeUnit.SECONDS)) {
				log.error("      processorPool timed out before terminating cleanly!");
			}
			else {
				log.info("      processorPool terminated cleanly (isTerminated = " + processorPool.isTerminated() + ", isShutdown = " + processorPool.isShutdown() + ")");
				List<Runnable> notRunJobs = processorPool.shutdownNow();
				log.info("      didn't run " + notRunJobs.size());
				processorPool = null;
			}
			
		
		} catch (InterruptedException e) {
			log.error("    Shutting down pools interrupted!");
		}
		
		setWorkerAcceptingNew(false);
		
		setWorkerStatus("down");
		
		//
		// DEBUGGING LOGGING
		// REMOVE ONCE ALL ISSUES WORKED OUT
		//
		threadSet = Thread.getAllStackTraces().keySet();
		i = threadSet.iterator();
		threadNum = 0;
		while(i.hasNext()) {
			Thread t = i.next();
			log.info("B THREAD(" + (++threadNum) + ") = " + t);
			if (t.getName().startsWith("procReq-") || t.getName().startsWith("pool-")) {
				log.info("interrupting " + t.getName() + " " + t.isAlive() + " " + t.isInterrupted() + " " + t.isDaemon() + " " + t.getClass());
				t.interrupt();
			}
		}
		threadSet = Thread.getAllStackTraces().keySet();
		i = threadSet.iterator();
		threadNum = 0;
		while(i.hasNext()) {
			Thread t = i.next();
			log.info("C THREAD (" + (++threadNum)  + ") " + t.getName() + " " + t.isAlive() + " " + t.isInterrupted() + " " + t.isDaemon() + " " + t.getClass());
		}
		
		// TODO: perform any other necessary actions
	}
	
	
	public String getWorkerId() {
		return workerId;
	}
}
