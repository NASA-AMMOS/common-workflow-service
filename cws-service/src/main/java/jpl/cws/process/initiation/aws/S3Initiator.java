package jpl.cws.process.initiation.aws;

import jpl.cws.core.code.CodeService;
import jpl.cws.partner.finding.custom.S3PartnerFinder;
import jpl.cws.process.initiation.CwsProcessInitiator;
import org.python.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This initiator is an implementation that will determine whether the
 * conditions are right for a process to be scheduled for execution.
 *
 * This initiator watches the specified s3BucketName for objects that match
 *
 * A partner finder implementation for this initiator can be plugged in,
 * and if so, it will attempt to look for partners in the specified s3BucketName.
 *
 * If a partner finder is plugged in, this initiator will only fire when all partners
 * have been found in the specified s3BucketName within the given partnerCollectionPeriod
 *
 * Use the following configuration in your initiators.xml file for this initiator:
 *   class="jpl.cws.process.initiation.aws.S3Initiator"
 *
 * @author ghollins, ztaylor
 *
 */
public class S3Initiator extends CwsProcessInitiator implements InitializingBean, ApplicationContextAware {
	private static final Logger log = LoggerFactory.getLogger(S3Initiator.class);

	@Value("${cws.console.hostname}") private String consoleHostname;
	@Value("${cws.console.port.ssl}") private String consoleSslPort;
	@Value("${aws.default.region}") private String aws_default_region;
	@Autowired private ApplicationContext springContext;
	@Autowired public CodeService cwsCodeService;

	final ThreadFactory schedThreadFactory = new ThreadFactoryBuilder()
			.setNameFormat("sched-%d")
			.setDaemon(true)
			.build();
	private ExecutorService schedPool;

	//
	// NOTE:
	// ctor<name> variables in this class are used to store original constructor values,
	// 			  used when spring reloads/refreshes this bean
	//

	//
	//
	// Partner finder implementation
	//
	private static final String DEFAULT_PARTNER_FINDER_IMPL = "jpl.cws.partner.finding.custom.SingleFinder";
	private String partnerFinderImpl;
	private String ctorPartnerFinderImpl;

	//
	// Mapping between variable name and pattern of S3 object to match
	//
	private Map<String, String> s3ObjPatterns;
	private Map<String, String> ctorS3ObjPatterns;

	// Used if snippets method specified for partner finding
	//
	private Method snippetsMethod;
	private Object snippetsClassObj;

	// Used for class-based partner finding
	//
	private S3PartnerFinder partnerFinder;

	// The S3 bucket name.
	//  This is where objects will be looked for (e.g. partners)
	//
	private String s3BucketName, ctorS3BucketName;

	// The period of time to wait to collect partners
	// after the initial trigger object starts the collection
	// period.
	//
	private Long partnerCollectionPeriod, ctorPartnerCollectionPeriod;

	// Time-limited list of processed hash codes.
	// This is the time in which the exact same (based on AWS etags)
	// products can't be processed more than once.
	// Outside of this time window, they can be processed again.
	//
	// TODO: It's not necessary to use a Map type here.
	//       A Set type is probably more appropriate.
	private static final int DUPLICATE_PREVENTION_PERIOD = 600; // 10 minutes
	private static TtlHashMap<Integer, Integer> recentlyProcessedInputs
			= new TtlHashMap(TimeUnit.SECONDS, DUPLICATE_PREVENTION_PERIOD);

	// Time-limited list of processed inputs:
	//             KEY                                VALUE
	//   <HASHCODE:initiatorId+s3ObjUrl, HASHCODE:initiatorId+s3ObjUrl>
	//
	// TODO: It's not necessary to use a Map type here.
	//       A Set type is probably more appropriate.
	private static TtlHashMap<Integer, Integer> individualProcessedInputs
			= new TtlHashMap(TimeUnit.SECONDS, DUPLICATE_PREVENTION_PERIOD * 10);

	public S3Initiator() {
	}  // needed by Spring for construction


	@Override
	public boolean isValid() {
		log.trace("isValid()...");
		if (s3BucketName == null) {
			log.warn("s3BucketName not specified!");
			return false;
		}
		if (ctorS3BucketName == null) {
			log.warn("ctorS3BucketName not specified!");
			return false;
		}
		if (partnerFinderImpl == null) {
			log.warn("partnerFinderImpl not specified!");
			return false;
		}
		if (ctorPartnerFinderImpl == null) {
			log.warn("ctorPartnerFinderImpl not specified!");
			return false;
		}
		if (s3ObjPatterns == null) {
			log.warn("s3ObjPatterns not specified!");
			return false;
		}
		if (ctorS3ObjPatterns == null) {
			log.warn("ctorS3ObjPatterns not specified!");
			return false;
		}
		if (cwsCodeService == null) {
			log.warn("cwsCodeService not initialized!");
			return false;
		}
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.trace("S3Initiator::afterPropertiesSet()... " + springContext);

		//
		// There are two main Spring context files involved here.
		// One is the master web application WEB-INF/applicationContext.xml.
		// The other is the initiators XML.
		// This code is probably called as a result of loading the latter XML,
		// and if so, the parent (applicationContext.xml) might be set.
		// If cwsCodeService is not already set, we need to
		// get the cwsCodeService from that context.
		//
		if (cwsCodeService == null) {
			if (springContext.getParent() == null) {
				cwsCodeService = (CodeService) springContext.getBean("cwsCodeService");
				log.debug("post cwsCodeService (from child) = " + cwsCodeService);
			} else {
				cwsCodeService = (CodeService) springContext.getParent().getBean("cwsCodeService");
				log.debug("post cwsCodeService (from parent) = " + cwsCodeService);
			}
		}
		// Validate that procDefKey is set
		//
		if (cwsCodeService == null) {
			log.error("afterPropertiesSet: cwsCodeService not initialized!");
			valid = false;
		} else {
			log.debug("cwsCodeService = " + cwsCodeService);
		}

		if (partnerFinderImpl == null || partnerFinderImpl.isEmpty()) {
			log.info("partnerFinderImpl was null or empty!  Using default partnerFinderImpl of: " + DEFAULT_PARTNER_FINDER_IMPL);
			partnerFinderImpl = DEFAULT_PARTNER_FINDER_IMPL;
		}

		if (partnerFinderImpl.startsWith("snippets.")) {
			log.debug("partnerFinderImpl starts with snippets...");

			// Construct the usable snippet method
			//
			try {
				URL[] urls = CodeService.urls.toArray(new URL[0]);
				Class<?> snippetsClass = getCwsSnippetsCompiledClass(urls);
				Constructor<?> snippetsClassConstructor = snippetsClass.getConstructor();
				snippetsClassObj = snippetsClassConstructor.newInstance();
				String snippetsMethodName = this.partnerFinderImpl.substring(9);
				log.debug("Getting snippetsMethodName = " + snippetsMethodName);
				snippetsMethod = snippetsClassObj.getClass().getDeclaredMethod(snippetsMethodName, String.class, String.class);
			} catch (Exception e) {
				log.error("Problem locating and/or instantiating jpl.cws.core.code.Snippets class!", e);
				valid = false;
				return;
			}
		} else {
			// instantiate and initialize the PartnerFinder class
			//
			try {
				Class<?> partnerFinderClass = Class.forName(this.partnerFinderImpl);
				Constructor<?> partnerFinderClassConstructor = partnerFinderClass.getConstructor();
				partnerFinder = (S3PartnerFinder) partnerFinderClassConstructor.newInstance();
				Map<String, Object> finderConfig = new HashMap<>();
				partnerFinder.initialize(finderConfig);
				log.debug("groupFinder is " + partnerFinder);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				log.error("Failed to instantiate group finder plugin: " + e.getMessage());
			}
			//log.error("partnerFinderImpl does not start with 'snippets.'!  This is not supported");
		}

		schedPool = Executors.newFixedThreadPool(16, schedThreadFactory);
	}


	@Override
	public void run() {

		while (true) {
			try {
				log.debug("S3Initiator (" + initiatorId + ") still alive...");
				Thread.sleep(120000); // 2 minutes

				synchronized (this) {
					// Clear expired periodically, to avoid memory build up
					recentlyProcessedInputs.clearExpired();
					individualProcessedInputs.clearExpired();
				}

			} catch (InterruptedException e) {
				log.error("interrupted", e);
				break;
			}
		} // end while (true)

	}

	/**
	 * Schedule, if conditions met.
	 * Delegates to more specific scheduling logic.
	 */
	protected void scheduleIfConditionsMet(String triggerS3Obj) throws Exception {
		scheduleNormal(triggerS3Obj);

		// TODO: Schedule custom if custom code is present
	}

	/**
	 * Schedule with custom logic
	 */
	private void scheduleCustom(String triggerS3Obj) throws Exception {
		// TODO: Add custom code before process is scheduled normally (e.g. to set variables)
	}


	/**
	 * Scheduling logic for most types of process definitions...
	 *
	 */
	private void scheduleNormal(String triggerS3Obj) throws Exception {
		// Find all partners
		//
		Map<String,String> partnersVarMap = buildPartnersVariableMap(triggerS3Obj, true);
		Map<String,String> procVars = new HashMap<>(partnersVarMap);
		procVars.putAll(this.procVariables);

		if (partnerCollectionPeriod != null) {

			if (partnersVarMap.size() != s3ObjPatterns.size()) {
				log.debug("not scheduling since full set of partners not found!!");
				return;
			}

			boolean processImmediately = false;

			// Determine if at least one of the inputs was seen before
			//
			boolean inputSeenBefore = false;
			for (String partner : partnersVarMap.values()) {
				int hashCode = (initiatorId + partner).hashCode();
				if (individualProcessedInputs.get(hashCode) != null) {
					inputSeenBefore = true;
					break;
				}
			}

			// If we have never seen these inputs before, then
			// just go ahead and process this data immediately
			//
			if (!inputSeenBefore) {
				processImmediately = true;
			}

			if (processImmediately) {
				log.debug("[" + initiatorId + "] PROCESSING INPUTS IMMEDIATELY");
				scheduleIfNotAlready(triggerS3Obj, partnersVarMap, procVars,false);
			} else {
				// If the initiator has a partnerCollectionPeriod specified,
				// then wait for this period of time.
				// This is to avoid unnecessary scheduling of process definitions,
				// when multiple versions of the same product come in, in a short
				// period of time.
				//
				final String tObj = triggerS3Obj;
				schedPool.execute(
						new Thread(() -> {
							log.debug("[" + initiatorId + "] DELAYING PROCESSING FOR " + (partnerCollectionPeriod / 1000.0) + " sec");
							try {
								Thread.sleep(partnerCollectionPeriod);
							} catch (InterruptedException e) {
								log.error("thread interrupted during partnerCollectionPeriod 2", e);
							}

							// Schedule now, after wait
							try {
								Map<String, String> updatedPartnersVarMap = buildPartnersVariableMap(triggerS3Obj, true);
								Map<String,String> updatedProcVars = new HashMap<>(partnersVarMap);
								updatedProcVars.put("s3BucketName", s3BucketName);
								updatedProcVars.putAll(procVariables);
								scheduleIfNotAlready(triggerS3Obj, updatedPartnersVarMap, updatedProcVars,false);
							} catch (Exception e) {
								log.error("Error in sched thread", e);
							}
						})
				);
			}
		} else {
			// don't wait for partnerCollectionPeriod, just go!
			scheduleIfNotAlready(triggerS3Obj, partnersVarMap, procVars,false);
		}
	}

	/**
	 *
	 *
	 */
	private void scheduleIfNotAlready(String initiationTrigger, Map<String,String> partnersMap, Map<String, String> procVars, boolean skipPartnerCountCheck) throws Exception {

		// partners should also contain s3ObjName, so should include all
		if ((partnersMap.size() == s3ObjPatterns.size()) || skipPartnerCountCheck) {

			// Don't schedule if inputs have already been processed
			//
			if (skipScheduling(partnersMap)) {
				log.debug("not scheduling, since a process has already been scheduled for this set of inputs.");
				return;
			}

			// Add in last minute variables
			//
			procVars.put("s3BucketName", s3BucketName);
			procVars.put("s3Obj", initiationTrigger);

			String initiationKey = "";
			for (Map.Entry<String,String> entry : partnersMap.entrySet()) {
				if (initiationKey.length() > 0) {
					initiationKey += "<br/>";
				}
				initiationKey += entry.getKey() + " = " + entry.getValue();
			}

			// Schedule the process in CWS
			//
			log.info("About to schedule " + procDefKey + " ... variables = " + procVars);
			this.cwsScheduler.scheduleProcess(procDefKey, procVars, procBusinessKey, initiationKey, Integer.parseInt(procPriority));
		}
		else {
			log.debug("not scheduling '" + procDefKey + "', since full set of partners not found!! (expected " +
					s3ObjPatterns.size() + ", but found " + partnersMap.size());
		}
	}


	/**
	 * Returns true, if scheduling should be skipped, due to
	 * the inputs having already been (recently) processed.
	 *
	 * S3 is checked for actual contents of objects at the time of this call.
	 *
	 * The way we determine whether the inputs have been processed already is by
	 * creating a unique hash code of the partners eTags and the initiatorId,
	 * and looking at in-memory history to see if they have already been processed
	 * (hash code will already exist in in-memory storage).
	 *
	 * Background:  the eTag is a hash of the S3 object.
	 *              The ETag reflects changes only to the contents of an object,
	 *              not its metadata
	 */
	protected synchronized boolean skipScheduling(Map<String,String> partners)  {
		S3DataManager s3 = new S3DataManager(aws_default_region);

		// Get sorted list of s3ObjKey/etags for all partners
		List<String> eTagList = new ArrayList<>();
		for (String s3ObjKey : partners.values()) {
			HeadObjectResponse metaData = s3.getObjectMetadata(s3BucketName, s3ObjKey);
			if (metaData != null) {
				eTagList.add(s3ObjKey + metaData.eTag());
			}
			else {
				log.warn("Skipping scheduling process for inputs: " + partners +
						", since they don't all exist for this initiator (" + initiatorId + ")");
				return true; // not all objects exist, so skip scheduling
			}
		}

		// hashCode is different, depending on order, so sort
		Collections.sort(eTagList);

		// get hashcode
		int hashCode = ( initiatorId + eTagList.toString() ).hashCode();
		if (recentlyProcessedInputs.get(hashCode) != null) {
			log.info("Skipping scheduling process for inputs: " + partners +
				", since they have been recently scheduled (" + hashCode + ") for this initiator (" + initiatorId + ") " +
				"within the last " + DUPLICATE_PREVENTION_PERIOD + " seconds.");
			return true; // already processed this set of inputs
		}
		else {
			recentlyProcessedInputs.put(hashCode, hashCode);
			// also add in, for each partner, a hashcode into another (new) TTL map
			//		Then check this map in other parts of code .
			//		If none are found in mem in other part of code, then schedule immediately.
			// this avoids the false positive of "old".
			// Also, cleanup models , like XYZ to produce new RDR versions...
			log.debug("added hash code: " + hashCode + ", to recentlyProcessedInputs. " +
					recentlyProcessedInputs.size() + " (initiatorId = " + initiatorId + ")");

			for (String partner : partners.values()) {
				hashCode = (initiatorId + partner).hashCode();
				individualProcessedInputs.put(hashCode, hashCode);
			}
			return false;
		}
	}


	/**
	 * Finds and returns the set of partners, INCLUDING the original triggering S3 object.
	 * This is a mapping of <[variable Name], [S3 obj name]>
	 *
	 */
	private Map<String,String> buildPartnersVariableMap(String triggerS3Obj, boolean includeTriggerObj) throws Exception {
		Map<String,String> partnersMap = new HashMap<>();

		List<String> partnerStrings = findPartnerStrings(triggerS3Obj);
		if( includeTriggerObj ) {
			partnerStrings.add(triggerS3Obj); // add in the original trigger object
		}

		//
		// Iterate over the partners and find the matching key/pattern,
		// Add these as key/value pairs to the set of process variables.
		//
		for (String partnerS3ObjName : partnerStrings) {
			//log.trace("[" + initiatorId + "] PARTNER:  " + partnerS3ObjName);
			String variableName = getMatchingPattern(partnerS3ObjName);
			if (variableName != null) {
				partnersMap.put(variableName, partnerS3ObjName);
			}
		}

		return partnersMap;
	}


	/**
	 * Uses the partner finder implementation to look for (on S3), the
	 * expected partners.
	 *
	 * The list returned does NOT include the trigger object
	 *
	 */
	private List<String> findPartnerStrings(String triggerS3Obj) throws Exception {
		List<String> partnerStrings = null;
		if (snippetsMethod != null) {
			//
			// SNIPPETS-METHOD-BASED PARTNER FINDING
			//
			try {
				log.debug("About to invoke partner finding snippet method: " + snippetsMethod + " (" + triggerS3Obj + ", " + s3BucketName + ")...");

				partnerStrings = (List<String>) snippetsMethod.invoke(snippetsClassObj, triggerS3Obj, s3BucketName);
				if (partnerStrings != null) {
					for (String p : partnerStrings) {
						log.debug("[" + initiatorId + "]  snippet (" + snippetsMethod + ") found a partner: " + p);
					}
				} else {
					log.debug("[" + initiatorId + "]  snippet (" + snippetsMethod + ") returned null for partners");
				}
			} catch (Exception e) {
				log.error("[" + initiatorId + "]  problem invoking snippet method " + snippetsMethod + " for partner finding!", e);
				return null;
			}
		}
		else {
			//
			// CLASS-BASED PARTNER FINDING
			//
			if (partnerFinder == null) {
				throw new Exception("groupFinder is null!");
			}
			log.debug("About to invoke class-based partner finder " + partnerFinder.getClass().getSimpleName() + ".getInputPartners method " +
					"with parameters (" + triggerS3Obj + ", " + s3BucketName + ")...");
			partnerStrings = partnerFinder.getInputPartners(triggerS3Obj, s3BucketName);
		}

		return partnerStrings;
	}


	/**
	 * Returns the key for the matching pattern, null otherwise.
	 *
	 */
	private String getMatchingPattern(String test) {
		for (String key : s3ObjPatterns.keySet()) {
			String pattern = s3ObjPatterns.get(key);
			if (test.matches(pattern)) {
				return key; // found a match
			}
		}
		// If no pattern matches
		return null;
	}


	public String getPartnerFinderImpl() {
		return partnerFinderImpl;
	}


	/**
	 * Sets the partnerFinderImpl
	 *
	 */
	public void setPartnerFinderImpl(String partnerFinderImpl) {
		log.debug("setPartnerFinderImpl(" + partnerFinderImpl + ")...");
		this.ctorPartnerFinderImpl = this.partnerFinderImpl = partnerFinderImpl;
	}


	/**
	 *
	 */
	private Class getCwsSnippetsCompiledClass(URL[] urls) throws IOException {
		// Load a class from the compiled code
		Class clazz = null;
		try {
			clazz = cwsCodeService.getCompiledClass();
			log.trace("LOADED CLASS (using CodeService): " + clazz);
		} catch (Exception e) {
			log.error("Unexpected exception while getting snippets compiled class", e);
		}
		return clazz;
	}


	@Override
	public MutablePropertyValues getSpecificPropertyValues() {
		MutablePropertyValues propVals = new MutablePropertyValues();
		propVals.add("s3BucketName",             ctorS3BucketName);
		propVals.add("partnerFinderImpl",        ctorPartnerFinderImpl);
		propVals.add("s3ObjPatterns",            ctorS3ObjPatterns);
		propVals.add("partnerCollectionPeriod",  ctorPartnerCollectionPeriod);
		return propVals;
	}


	@Override
	public void reapplySpecificProps() {
		setS3BucketName(ctorS3BucketName);
		setPartnerFinderImpl(ctorPartnerFinderImpl);
		setS3ObjPatterns(ctorS3ObjPatterns);
		setPartnerCollectionPeriod(ctorPartnerCollectionPeriod);
	}


	public String getS3BucketName() {
		return s3BucketName;
	}
	public void setS3BucketName(String s3BucketName) {
		this.s3BucketName = this.ctorS3BucketName = s3BucketName;
	}

	public void setCwsCodeService(CodeService cwsCodeService) {
		log.debug("setting cwsCodeService to " + cwsCodeService);
		this.cwsCodeService = cwsCodeService;
	}

	public Map<String,String> getS3ObjPatterns() { return s3ObjPatterns; }
	public void setS3ObjPatterns(Map<String, String> s3ObjPatterns) {
		this.s3ObjPatterns = this.ctorS3ObjPatterns = s3ObjPatterns;
	}

	public Long getPartnerCollectionPeriod() { return partnerCollectionPeriod; }
	public void setPartnerCollectionPeriod(Long partnerCollectionPeriod) {
		if (partnerCollectionPeriod != null) {
			log.debug("setting partnerCollectionPeriod to " + partnerCollectionPeriod);
		}
		this.partnerCollectionPeriod = this.ctorPartnerCollectionPeriod = partnerCollectionPeriod;
	}

	@Override
	public void setApplicationContext(ApplicationContext appContext) throws BeansException {
		springContext = appContext;
	}

}
