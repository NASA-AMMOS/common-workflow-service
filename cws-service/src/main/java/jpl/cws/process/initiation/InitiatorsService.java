package jpl.cws.process.initiation;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import jpl.cws.core.db.SchedulerDbService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.type.filter.AssignableTypeFilter;

import jpl.cws.core.service.SpringApplicationContext;
import jpl.cws.service.CwsConsoleService;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class InitiatorsService implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(InitiatorsService.class);
	
	@Autowired private CwsConsoleService cwsConsoleService;
	@Autowired private ApplicationContext applicationContext;
	@Autowired protected SchedulerDbService cwsSchedulerDbService;
	
	private static final String INITIATORS_XML_STABLE_FILE = "cws-process-initiators.xml";
	private static final String INITIATORS_XML_WORKING_FILE = "cws-process-initiators.work.xml";
	
	public InitiatorsService() {
		log.trace("InitiatorsService constructor...");
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.trace("afterPropertiesSet");
	}
	
	
	/**
	 * Gets the current XML context file.
	 * If the User is working on a currently invalid file, this will return
	 * INITIATORS_XML_WORKING_FILE, otherwise it will return INITIATORS_XML_FILE
	 * 
	 */
	public String getCurXmlContextFile() throws Exception {
		String fileContents = getWorkingFileContents();
		if (fileContents != null) {
			log.trace("returning working file contents...");
			return fileContents;
		}
		else {
			log.error("NO " + INITIATORS_XML_WORKING_FILE + " available!");
			return "ERROR: CONTENTS NOT AVAILABLE";
		}
	}
	
	private String getWorkingFileContents() throws Exception {
		return getFileContents(INITIATORS_XML_WORKING_FILE);
	}

	private String getStableFileContents() throws Exception {
		return getFileContents(INITIATORS_XML_STABLE_FILE);
	}
	
	
	/**
	 * 
	 */
	private String getFileContents(String classPathResource) throws Exception {
		try (InputStream in = this.getClass().getClassLoader()
			.getResource(classPathResource).openStream()) { // getResourceAsStream() caches, use this instead
			if (in == null) {
				log.warn("Not able to locate resource: " + classPathResource);
				return null;
			}
			return IOUtils.toString(in, "UTF-8");
		}
	}
	
	
	/**
	 * Returns a mapping of the current set of initiators.
	 * 
	 */
	public Map<String,CwsProcessInitiator> getProcessInitiators() {
		return SpringApplicationContext.getBeansOfType(CwsProcessInitiator.class);
	}


	/**
	 * Loads initiators from Stable file
	 *
	 */
	public void loadInitiators() throws Exception {

		log.debug("Loading initiators from stable file " + INITIATORS_XML_STABLE_FILE);

		try {
			String xmlContents = getStableFileContents();
			updateAndRefreshInitiators(xmlContents);
		} catch (Exception e) {
			log.error("Problem while loadInitiators()", e);

			throw e;
		}
	}


	/**
	 * This gets called when User clicks on SAVE button in UI
	 * 
	 */
	public void updateAndRefreshInitiators(String newXmlContext) throws Exception {
		//
		// Overwrite "working" file with new contents
		//
		log.debug("updating " + INITIATORS_XML_WORKING_FILE);
		writeXmlContextFile(newXmlContext, INITIATORS_XML_WORKING_FILE);
		
		try {
			// Attempt to refresh initiators from new XML file
			//
			log.debug("refreshing from " + INITIATORS_XML_WORKING_FILE);
			refreshInitiators(INITIATORS_XML_WORKING_FILE);
			
			// If we got here, it means XML was valid.
			// So write out the working XML to the STABLE XML
			log.debug("Valid, so writing to " + INITIATORS_XML_STABLE_FILE);
			writeXmlContextFile(newXmlContext, INITIATORS_XML_STABLE_FILE);
		} catch (Exception e) {
			// If there was a problem, then refresh beans based on STABLE XML
			//
			log.warn("Initiators XML invalid (" + e.getMessage() + "), so refreshing from last known stable XML: " + INITIATORS_XML_STABLE_FILE, e);
			refreshInitiators(INITIATORS_XML_STABLE_FILE);
			
			throw e;
		}
	}

	
	/**
	 * 
	 */
	private void writeXmlContextFile(String newXmlContext, String contextXmlResource) throws URISyntaxException {
		URL xmlContextUrl = this.getClass().getClassLoader().getResource(contextXmlResource);
		File oldXmlContextFile = new File(xmlContextUrl.toURI());
		String xmlContextFileAbsPath = oldXmlContextFile.getAbsolutePath();
		oldXmlContextFile.delete();

		File newXmlContextFile = new File(xmlContextFileAbsPath);
		
		try (FileWriter f2 = new FileWriter(newXmlContextFile, false)) {
			f2.write(newXmlContext);
			f2.flush();
		} catch (Exception e) {
			log.error("Unexpected problem", e);
		}
	}


	/**
	 *
	 */
	private void waitForWorkingFileWrite(String newXmlContext) throws Exception {

		boolean match = false;

		for (int i = 0, limit = 50; i < limit; i++) {

			if (newXmlContext.equals(getWorkingFileContents())) {
				match = true;
				break;
			}

			log.debug("Waiting for work XML file to update...");
			Thread.sleep(300);
		}

		if (!match) {
			String msg = "Working initiator xml file could not be updated.  Try saving again.";
			log.error(msg);
			throw new Exception(msg);
		}
	}


	/**
	 * TODO: Make this unnecessary with UI changes
	 *
	 * Compare new XML with stable XML and call replaceSingleInitiator()
	 * on each of the beanNames which have been changed
	 *
	 * This gets called when User clicks on SAVE button in UI
	 */
	public void updateChangedInitiators(String newXmlContext) throws Exception {
		//
		// Overwrite "working" file with new contents
		//
		log.debug("Updating changed initiators from " + INITIATORS_XML_WORKING_FILE);
		writeXmlContextFile(newXmlContext, INITIATORS_XML_WORKING_FILE);

		waitForWorkingFileWrite(newXmlContext);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		// Load the files
		Document workingDocument = db.parse(new InputSource(new StringReader(newXmlContext)));
		Document stableDocument  = db.parse(new InputSource(new StringReader(getStableFileContents())));

		// Get the bean definitions
		NodeList newNodeList = workingDocument.getElementsByTagName("bean");
		NodeList oldNodeList = stableDocument.getElementsByTagName("bean");

		log.debug("Number of beans in old doc: " + oldNodeList.getLength());
		log.debug("Number of beans in new doc: " + newNodeList.getLength());

		// Create Map (beanName -> xml)
		HashMap<String, String> newBeans = buildBeanNameXmlMap(newNodeList);
		HashMap<String, String> oldBeans = buildBeanNameXmlMap(oldNodeList);

		// Find deleted initiators (in the old xml but not in the new one)
		ArrayList<String> deletedBeans = new ArrayList<>();
		oldBeans.forEach((k, v) -> {
			if (!newBeans.containsKey(k)) {
				deletedBeans.add(k);
			}
		});

		log.debug("Num deleted: " + deletedBeans.size());

		// Delete these beans
		deletedBeans.forEach(this::disableAndRemoveInitiatorBean);

		ArrayList<CwsProcessInitiator> initiators = new ArrayList<>();
		HashMap<String, String> invalidMessages = new HashMap<>();

		// For each bean in the new xml file, check if it matches
		// its old definition and replace it if it doesn't
		for (Map.Entry<String, String> entry : newBeans.entrySet()) {
			String beanName = entry.getKey();
			String beanXmlStr = entry.getValue();

			// Is this a new initiator?
			if (!oldBeans.containsKey(beanName)) {
				// Add this one
				try {
					log.debug("Initiator with id " + beanName + " is a new initiator, adding.");
					replaceSingleInitiator(INITIATORS_XML_WORKING_FILE, beanName);
				} catch (Exception e) {
					log.error("Error replacing bean with id: " + beanName + ", skipping.", e);
					invalidMessages.put(beanName, e.getMessage());
				}
			}
			else {
				String oldXml = oldBeans.get(beanName);

				// Compare
				Diff diff = DiffBuilder.compare(oldXml).withTest(beanXmlStr).build();

				// If this initiator has been edited
				if (diff.hasDifferences()) {
					try {
						log.debug("Initiator with id " + beanName + " is changed, replacing");
						replaceSingleInitiator(INITIATORS_XML_WORKING_FILE, beanName);
						initiators.add(cwsConsoleService.getProcessInitiatorById(beanName));
					} catch (Exception e) {
						log.error("Error replacing bean with id: " + beanName + ", skipping.", e);
						invalidMessages.put(beanName, e.getMessage());
					}
				}
				else {
					log.debug("Initiator with id " + beanName + " was unchanged, skipping.");
				}
			}
		}

		if (!invalidMessages.isEmpty()) {
			throw new Exception("One or more initiators invalid: " + invalidMessages);
		}

		// If we got here, there were no issues. Write to stable file
		log.debug("Valid, so writing to " + INITIATORS_XML_STABLE_FILE);
		writeXmlContextFile(newXmlContext, INITIATORS_XML_STABLE_FILE);
	}


	/**
	 * Builds a map of beanName -> xml
	 */
	private HashMap<String, String> buildBeanNameXmlMap(NodeList nl) {

		HashMap<String, String> map = new HashMap<>();

		// Iterate through the nodes
		for(int x = 0, size = nl.getLength(); x<size; x++) {
			Node node = nl.item(x);
			String beanName = node.getAttributes().getNamedItem("id").getNodeValue();
			String nodeStr = nodeToString(node);

			map.put(beanName, nodeStr);
		}

		return map;
	}


	/**
	 * Convert a DOM Node to an XML String for comparison with XMLUnit
	 *
	 * Source: https://stackoverflow.com/questions/4412848/xml-node-to-string-in-java
	 */
	private String nodeToString(Node node) {
		try (StringWriter sw = new StringWriter()) {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
			return sw.toString();
		} catch (TransformerException te) {
			log.debug("InitiatorService: nodeToString Transformer Exception", te);
			return null;
		} catch (IOException e) {
			log.error("Exception in nodeToString", e);
			return null;
		}
	}


	/**
	 * TODO: Switch over to this after UI changes allow editing of a single initiator
	 *
	 * This will be called in the REST service when a user edits
	 * a single initiator configuration in the new UI.
	 */
	public void updateSingleInitiator(String newXmlContext, String beanName) throws Exception {
		//
		// Overwrite "working" file with new contents
		//
		log.debug("Updating " + beanName + " from " + INITIATORS_XML_WORKING_FILE);
		writeXmlContextFile(newXmlContext, INITIATORS_XML_WORKING_FILE);

		try {
			// Attempt to refresh initiators from new XML file
			//
			log.debug("Refreshing from " + INITIATORS_XML_WORKING_FILE);
			replaceSingleInitiator(INITIATORS_XML_WORKING_FILE, beanName);

			// If we got here, it means XML was valid.
			// So write out the working XML to the STABLE XML
			log.debug("Valid, so writing to " + INITIATORS_XML_STABLE_FILE);
			writeXmlContextFile(newXmlContext, INITIATORS_XML_STABLE_FILE);
		} catch (Exception e) {
			// If there was a problem, the parent context will remain unchanged
			//
			log.warn("Initiators XML invalid (" + e.getMessage() + "), leaving application context unchanged", e);

			throw e;
		}
	}


	/**
	 * This will load a new XML context and swap the requested bean into the parent
	 * ApplicationContext (if one exists) without refreshing all initiators. If there
	 * is no existing bean which matches the given key, a new bean will be added instead.
	 *
	 * If there is an issue with the initiator configuration, the existing initiator with
	 * the same key (if it exists) will remain unchanged.
	 *
	 * @param contextXmlResource The XML file containing the new bean definition
	 * @param beanName The name for the specific bean to replace/add in the new context
	 * @throws Exception
	 */
	private void replaceSingleInitiator(String contextXmlResource, String beanName) throws Exception {

		// Disable the initiator we are about to replace
		boolean enabled = disableAndRemoveInitiatorBean(beanName);

		log.debug("Swapping out bean with name: " + beanName);
		log.debug("Enabled = " + enabled);
		log.debug("Loading new context file (" + contextXmlResource + ")...");

		// Load new initiators context from file (using parent application context as well)
		String[] con = new String[] { contextXmlResource };
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(con, applicationContext);

		log.debug("New context beans = " + Arrays.toString(context.getBeanDefinitionNames()));

		// Swap this new bean into the parent applicationContext
		CwsProcessInitiator initiator = (CwsProcessInitiator)context.getBean(beanName);

		// Make sure it is valid before swapping it in
		if (!validateInitiator(initiator)) {
			String msg = "Initiator with id + " + initiator.initiatorId + " is invalid! Skipping. ";
			log.debug(msg);
			// This message is checked for in the UI, do not change it
			throw new Exception(msg + "Reason: no row for procDef '" + initiator.procDefKey + "' exists in DB!");
		}

		// Swap it in
		cwsConsoleService.replaceInitiatorBean(beanName, initiator);
		disableAndStopInitiator(initiator);

		// Do not need this context anymore
		context.close();

		// Get back the initiator
		initiator = cwsConsoleService.getProcessInitiatorById(initiator.getInitiatorId());

		// Restore enabled state
		if (enabled) {
			log.debug("Restoring initiator (" + initiator.getInitiatorId() + ") back to enabled...");
			enableAndStartInitiator(initiator);
		}
	}


	/**
	 * Checks if an initiator is valid before loading it into parent context
	 * @param initiator The candidate initiator
	 * @return whether the initiator is valid
	 */
	public boolean validateInitiator(CwsProcessInitiator initiator) {
		return cwsSchedulerDbService.engineProcessRowExists(initiator.procDefKey);
	}


	/**
	 * Disables, stops, and removes a single initiator bean.
	 *
	 * Returns whether the initiator was enabled
	 */
	private boolean disableAndRemoveInitiatorBean(String beanName) {
		log.trace("disableAndRemoveInitiatorBean...");

		// Default to false - if this fails, the new bean will start as disabled
		boolean enabled = false;
		boolean newBean = false;

		try {
			CwsProcessInitiator curInitiator = (CwsProcessInitiator) SpringApplicationContext.getBean(beanName);
			enabled = curInitiator.isEnabled();
			disableAndStopInitiator(curInitiator);
			log.trace("CURRENT INITIATOR (bean): " + curInitiator + " removing bean def...");
		}
		catch (NoSuchBeanDefinitionException e) {
			// This is a new bean, return false
			log.debug(beanName + " is a new bean, defaulting to false");

			enabled = false;
			newBean = true;
		}
		catch (Exception e) {
			log.error("Problem getting bean '" + beanName + "' back", e);
		}
		finally {
			log.trace("CURRENT INITIATOR: " + beanName + " removing bean def...");

			if (!newBean) {
				cwsConsoleService.removeBean(beanName);
			}
		}

		return enabled;
	}


	/**
	 * Disables, stops, and removes the current initiator beans.
	 * 
	 * Returns a map of the existing (valid) initiator bean names, and whether they were enabled.
	 * 
	 */
	private Map<String,Boolean> disableAndRemoveAllInitiatorBeans() {
		log.trace("disableAndRemoveAllInitiatorBeans...");
		Map<String,Boolean> existingInitiatorsEnabledMap = new HashMap<String,Boolean>();
		
		for(String beanName : cwsConsoleService.getBeanNamesOfType(CwsProcessInitiator.class)) {
			log.trace("OLD BEAN: " + beanName);
			try {
				CwsProcessInitiator curInitiator = (CwsProcessInitiator)SpringApplicationContext.getBean(beanName);
				existingInitiatorsEnabledMap.put(beanName, curInitiator.isEnabled());
				disableAndStopInitiator(curInitiator);
				log.trace("CURRENT INITIATOR (bean): " + curInitiator + " removing bean def...");
			}
			catch (Exception e) {
				log.error("Problem getting bean '" + beanName + "' back", e);
			}
			finally {
				log.trace("CURRENT INITIATOR: " + beanName + " removing bean def...");
				cwsConsoleService.removeBean(beanName);
			}
		
		}
		
		return existingInitiatorsEnabledMap;
	}
	
	
	/**
	 * 
	 */
	private void refreshInitiators(String contextXmlResource) throws Exception {
		// Before loading new set of initiators from XML,
		// get the current set of initiators, disable them, interrupt them, and remove them
		// Also, stores a map to keep track of what was previously enabled/disabled,
		// so these statuses can be restored after refresh.
		//
		Map<String,Boolean> existingInitiatorsEnabledMap = disableAndRemoveAllInitiatorBeans();
		
		log.trace("existingInitiatorsEnabledMap = " + existingInitiatorsEnabledMap);
		log.trace("loading new context file (" + contextXmlResource + ")...");
		
		// Load new initiators context from file (using parent application context as well)
		//
		String[] con = new String[] { contextXmlResource };
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(con, applicationContext);
		
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.addIncludeFilter(new AssignableTypeFilter(CwsProcessInitiator.class));
		
		Map<String,CwsProcessInitiator> curInitiators = new HashMap<String,CwsProcessInitiator>();
		
		// Scan for applicable classes to swap in new initiator beans with
		//
		Set<BeanDefinition> components = provider.findCandidateComponents("jpl");
		for (BeanDefinition component : components) {
			Class cls = Class.forName(component.getBeanClassName());
			
			// If the class is a sub-class of CwsProcessInitiator,
			// see if any beans are defined in new context..
			//
			if (CwsProcessInitiator.class.isAssignableFrom(cls)) {
				Map<String,Object> newCtxBeans = context.getBeansOfType(cls);

				// Replace beans with those in new context
				//
				for (Entry<String,Object> newCtxBean : newCtxBeans.entrySet()) {
					log.trace("NEW XML-DEFINED CONTEXT bean: " + newCtxBean.getKey() + ", " + newCtxBean.getValue());
					CwsProcessInitiator initiator = (CwsProcessInitiator)newCtxBean.getValue();
					cwsConsoleService.replaceInitiatorBean(newCtxBean.getKey(), initiator);
					disableAndStopInitiator(initiator);
					//curInitiators.put(newCtxBean.getKey(), initiator);
					log.trace("getting back initiator by id: "+initiator.getInitiatorId() + " curInitiators.size = " + curInitiators.size());
					curInitiators.put(newCtxBean.getKey(),
							cwsConsoleService.getProcessInitiatorById(initiator.getInitiatorId()));
					log.trace(" curInitiators.size now = " + curInitiators.size());
				}
			}
		}
		
		context.close();
		
		log.trace("existingInitiatorsEnabledMap = " + existingInitiatorsEnabledMap);
		
		Map<String,String> invalidMessages = new HashMap<String,String>();
		
		// Get current set of initiators and make sure they are all disabled
		//
		for (Entry<String,CwsProcessInitiator> curInitiator : curInitiators.entrySet()) {
			CwsProcessInitiator initiator = curInitiator.getValue();
			
			log.debug("XML-DEFINED INITIATOR: " + initiator);
			
			String invalidMsg = initiator.getInvalidMsg();
			if (!invalidMsg.isEmpty()) {
				invalidMessages.put(initiator.getInitiatorId(), invalidMsg);
			}
			else {
				// Initiator is valid.
				//
				// If the initiator was previously enabled,
				// then restore it back to its enabled state
				//
				Boolean previousState = existingInitiatorsEnabledMap.get(curInitiator.getKey());
				if (previousState != null && previousState == true) {
					log.debug("Restoring initiator (" + initiator.getInitiatorId() + ") back to enabled...");
					enableAndStartInitiator(initiator); // restore it's enabled state
				}
			}
		}
		
		if (!invalidMessages.isEmpty()) {
			throw new Exception("One or more initiators invalid: " + invalidMessages);
		}
		
	}
	
	// Synchronized to prevent race conditions in refreshing the spring context
	public synchronized void disableAndStopInitiator(String initiatorId) throws Exception {
		disableAndStopInitiator(cwsConsoleService.getProcessInitiatorById(initiatorId));
	}

	// Synchronized to prevent race conditions in refreshing the spring context
	public synchronized void enableAndStartInitiator(String initiatorId) throws Exception {
		enableAndStartInitiator(cwsConsoleService.getProcessInitiatorById(initiatorId));
	}
	
	
	public CwsProcessInitiator getInitiator(String initiatorId) {
		return cwsConsoleService.getProcessInitiatorById(initiatorId);
	}
	
	/**
	 * 
	 */
	private void disableAndStopInitiator(CwsProcessInitiator initiator) throws Exception {
		log.debug("disableAndStopInitiator " + initiator);
		if (initiator == null) {
			throw new IllegalArgumentException("null initiator!");
		}
		
		// Is the initiator currently enabled?
		//
		boolean isEnabled = initiator.isEnabled();
		log.trace("before updating initiator, enabled = " + isEnabled);
		
		if (isEnabled) {
			// Disable the initiator
			//
			initiator.setEnabled(false);
		}
		
		// Stop initiator, if necessary
		//
		if (initiator.isAlive()) {
			log.trace("Interrupting (stopping) initiator: " + initiator + " ...");
			
			// Interrupt the initiator...
			//
			initiator.interrupt();
			
			// Wait for initiator to die...
			//
			int maxWaits = 0;
			while (initiator.isAlive() && maxWaits++ < 10) {
				log.warn("initiator still alive... (poll #" + maxWaits + ")");
				try { Thread.sleep(200); } catch (InterruptedException e) { }
			}
			if (initiator.isAlive()) {
				log.error("initiator " + initiator + " (procDefKey=" +initiator.getProcDefKey()+") still alive after interruption!");
			}
			else {
				log.trace("initator is NOT alive.");
			}
		}
		else {
			log.trace("initiator was already dead. No need to interrupt it.");
		}
		
		initiator.cleanUp();
		log.trace("Done with disableAndStopInitiator of " + initiator);
	}
	
	
	/**
	 * 
	 */
	private void enableAndStartInitiator(CwsProcessInitiator initiator) throws Exception {
		log.debug("enableAndStartInitiator " + initiator);
		if (initiator == null) {
			throw new IllegalArgumentException("null initiator!");
		}

		// If this initiator is not being enabled for the first time, then
		// we must re-initialize it..
		//
		if (initiator.getState() != Thread.State.NEW) {
			disableAndStopInitiator(initiator);
			cwsConsoleService.replaceInitiatorBean(initiator.getInitiatorId(), initiator);
			initiator = cwsConsoleService.getProcessInitiatorById(initiator.getInitiatorId());
		}
		
		// Enable the initiator
		//
		initiator.setEnabled(true);

		// Start up initiator if necessary
		//
		try {
			if (initiator.isAlive()) {
				log.error("initiator should not already be alive!");
			}
			else {
				log.debug("Starting initiator: " + initiator + " ...");
				initiator.start();
			}
		}
		catch (Exception e) {
			log.error("problem while starting up a new initiator", e);
		}
	}

}
