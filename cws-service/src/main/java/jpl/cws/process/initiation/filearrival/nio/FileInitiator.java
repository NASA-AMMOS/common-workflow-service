package jpl.cws.process.initiation.filearrival.nio;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;

import jpl.cws.console.CwsStartupAndShutdown;
import jpl.cws.core.code.CodeService;
import jpl.cws.core.db.SchedulerJob;
import jpl.cws.process.initiation.CwsProcessInitiator;

public class FileInitiator extends CwsProcessInitiator implements MessageListener {

	private static final Logger log = LoggerFactory.getLogger(FileInitiator.class);
	
	//
	// Sets the mode that this initiator gets triggered by
	//
	public static HashMap<String,Thread> fileInitiatorThreads = new HashMap<String,Thread>();
	
	// directory that initiator watches for new files
	//
	private String rootDirStr;
	private String xtorRootDirStr;
	private Path rootDirPath;
	
	//
	//
	private Map<String,String> filePatterns;
	private Map<String,String> xtorFilePatterns;
	
	//
	// Partner finder implementation
	//
	private static final String DEFAULT_PARTNER_FINDER_IMPL = "jpl.cws.process.initiation.filearrival.nio.SingleFinder";
	private String partnerFinderImpl;
	private String xtorPartnerFinderImpl;
	
	//
	// Duration in seconds between directory polling
	//
	private static final int DEFAULT_POLL_PERIOD = 5;
	private int pollPeriod            = DEFAULT_POLL_PERIOD;
	private int xtorPollPeriod        = DEFAULT_POLL_PERIOD;
	
	private Path cacheDir;      // where files that match patterns are put, but still waiting for complete set of partners...
	private static boolean moveUnmatchedToUnmatched = false; // NOTE: only set to true, until an "ignore list" is implemented!!!
	
	private static DirectoryStream.Filter<Path> rootDirFilesOnlyFilter = new DirectoryStream.Filter<Path>() {
		public boolean accept(Path file) throws IOException {
			return Files.isSymbolicLink(file) || Files.isRegularFile(file);
		}
	};
	
	public static final String CACHE_DIR_NAME      = "cws_initiator_cache";
	
	private Set<String> nonMatchingFiles = new HashSet<String>();
	
	private int numMatches   = 0;
	private int numInitiated = 0;
	
	private GroupFinder_IF groupFinder;
	
	// Used if snippets method specified for partner finding
	//
	private Object snippetsClassObj;
	private Method snippetsMethod;
	
	private int SNAPSHOT_SIZE         = 1000;
	
	private int NUM_TRIES_BEFORE_WAIT = 1;  // Number of empty polls before waiting
	
	private URL[] urls;
	
	
	public FileInitiator() {}
	
	
	/**
	 * This method is called by the superclass, directly after this class is constructed.
	 * It sets the "valid" member field of the super class.
	 * 
	 */
	@Override
	public boolean isValid() {
		// Check whether root directory was specified
		//
		if (rootDirPath == null) {
			log.error("Must specify a root directory!");
			return false;
		}
		
		// Validate that directories actually exist
		//
		if (!Files.isDirectory(rootDirPath)) {
			log.error("Must specify a directory that exists! (specified: " + rootDirPath + ")");
			return false;
		}
		if (!Files.isDirectory(cacheDir)) {
			log.error("Must specify a directory that exists! (specified: " + cacheDir + ")");
			return false;
		}
		
		return true; // valid -- we are good to go!
	}
	
	
	@Override
	public void run() {
		
		if (!isValid()) {
			log.warn("Not running FileInitiator because it's not valid.");
			return;
		}
		
		fileInitiatorThreads.put(initiatorId, this.currentThread());
		
		List<Path> accepted = new ArrayList<Path>();
		
		log.info("FileInitiator (" + rootDirPath.toAbsolutePath() + ", " + procDefKey + ") starting...");
		long startMillis = 0;
		try {
			int emptyPollsBeforeWait = 0;
			
			while (true) {
				
				// TODO: Possibly add reapplyProps() here
				
				// Check whether initiator is still enabled.
				// If not, break out of main while loop. 
				if (!isEnabled()) {
					log.warn("Noticed that this process initiator is now disabled.  Stopping...");
					break;
				}
				
				// Get list of accepted and rejected files
				//
				accepted.clear();
				
				DirectoryStream<Path> rootDirFiles = Files.newDirectoryStream( rootDirPath, rootDirFilesOnlyFilter );
				try {
					startMillis = System.currentTimeMillis();
					int i = 0;
					for (Path path : rootDirFiles) {
						path = path.toAbsolutePath();
						String path_name = path.getFileName().toString();
						// Check if we've seen this file previously and determined it didn't match any initiators
						if (!nonMatchingFiles.contains(path_name))
						{
							// Check if filename matches pattern
							if (matches(path_name)) {
								log.debug("FileInitiator: matched rootDir file: " + path_name);
								accepted.add(path);
								if (++i > SNAPSHOT_SIZE) { break; }
							} else {
								nonMatchingFiles.add(path_name);
								log.debug("Added " + path_name + " to nonMatchingFiles, new size: " + nonMatchingFiles.size());
							}
						}

						if ( (System.currentTimeMillis()-startMillis)  > 10) {
							log.debug("FileInitiator ["+this.rootDirPath+"]: time to acquire snapshot = "+(System.currentTimeMillis()-startMillis) +"ms");
						}
					}
				} finally {
					if (rootDirFiles != null) {
						rootDirFiles.close();
						rootDirFiles = null;
					}
				}
				
				if (accepted.isEmpty()) {
					emptyPollsBeforeWait++; // empty poll -- increment counter
					
					// Sleep for a brief (escalating) period of time to avoid
					// thrashing of file system.
					//
					try { Thread.sleep(Math.min(emptyPollsBeforeWait*50, pollPeriod)); }
					catch (InterruptedException ie) {
						
						if (CwsStartupAndShutdown.isShuttingDown) {
							log.warn("Thread interrupted. Stopping thread, since it appears CWS is in the process of shutting down.");
							break;
						}
						else {
							log.debug("Thread interrupted. Continuing thread...");
							continue;
						}
					}
				}
				else {
					int numAccepted = accepted.size();

					// Got a list of matching products, process them
					startMillis = System.currentTimeMillis();
					
					if (numAccepted > 0) {
						processTriggerFiles(accepted);
					}
					log.debug("FileInitiator ["+this.rootDirPath+"]: time to process accepted["+numAccepted+"] = "+(System.currentTimeMillis()-startMillis)+"ms");
					continue;
				}
				
				// If we have had multiple, empty polls, then wait (backoff)
				//
				try {
					if (emptyPollsBeforeWait == NUM_TRIES_BEFORE_WAIT) {
						emptyPollsBeforeWait = 0;
						Thread.sleep(1000 * pollPeriod);
					}
				} catch (InterruptedException ie) {
					log.warn("Thread interrupted.");
					break;
				}
				
			} // end infinite while loop
			
		} catch (IOException ioe) {
			log.error(ioe.getMessage());
		}
		
		fileInitiatorThreads.remove(initiatorId);
		log.warn("Thread stopping.. Removed initiatorId '" + initiatorId + "' from fileInitiatorThreads.  Threads after remove: " + fileInitiatorThreads);
	}
	
	
	@Override
	public MutablePropertyValues getSpecificPropertyValues() {
		MutablePropertyValues propVals = new MutablePropertyValues();
		
		propVals.add("pollPeriod",        xtorPollPeriod);
		propVals.add("rootDir",           xtorRootDirStr);
		propVals.add("filePatterns",      xtorFilePatterns);
		propVals.add("partnerFinderImpl", xtorPartnerFinderImpl);
		
		return propVals;
	}
	

	@Override
	public void reapplySpecificProps() {
		
		setPollPeriod(xtorPollPeriod);
		setRootDir(xtorRootDirStr);
		setFilePatterns(xtorFilePatterns);
		setPartnerFinderImpl(xtorPartnerFinderImpl);
	}

	
	/*
	 * This method is intended to allow a process to "kick" an initiator
	 * so that the initiator can move on and process things.
	 * The process may have information, for example, that a file has been created
	 * somewhere, and this can avoid the latency involved with the polling 
	 * methodology in the run() method.
	 * 
	 * FIXME:  Currently, this approach might work if the thread being interrupted
	 *         is currently doing a Thread.sleep, but what happens if it's doing
	 *         something else?  It looks like the InterruptedException will bubble
	 *         out, and the thread will simply stop.  This is not what is intended.
	 *         So this feature is not yet fully implemented.  We need to distinguish
	 *         between the different interrupt cases (i.e. interrupt due to an
	 *         initiator being turned off, or interrupt due to an "kick").
	 */
	@Override
	public void onMessage(Message message) {
		if (message instanceof TextMessage) {
			try {
				log.info("----------------- GOT MESSAGE: "+message + " Redelivered?: "+ message.getJMSRedelivered());
				String idToInterrupt = message.getStringProperty("initiatorId");
				if (idToInterrupt != null) {
					Thread threadToInterrupt = fileInitiatorThreads.get(idToInterrupt);
					threadToInterrupt.interrupt();
				}
				else {
					log.error("idToInterrupt was null!!");
				}
			}
			catch (Exception e) {
				log.error("Exception onMessage", e);
				throw new RuntimeException(e); // FIXME: do we really want to do this?
			}
		}
		else {
			throw new IllegalArgumentException("Message must be of type TextMessage");
		}
	}
	
	
	/**
	 * @throws IOException 
	 * 
	 */
	private Class getCompiledClass() throws IOException {
		// Load a class from the compiled code
		Class clazz = null;
		URLClassLoader cl = null;
		try {
			cl = new URLClassLoader(urls, this.getClass().getClassLoader());
			clazz = cl.loadClass("jpl.cws.core.code.Snippets");
			log.info("LOADED CLASS: "+clazz);
		} catch (Exception e) {
			log.error("Unexpected exception", e);
		}
		finally {
			cl.close();
		}
		return clazz;
	}
		
	private void processTriggerFiles(List<Path> triggerFiles) throws IOException {
		log.info("PROCESSING: " + triggerFiles.size()+" trigger files...");
		
		// Process each trigger file
		//
		while (!triggerFiles.isEmpty()) {
			try {
				Path triggerFile = triggerFiles.get(0);
				triggerFile = rootDirPath.resolve(triggerFile);
				log.trace("Detected new path created: " + triggerFile);
				
				// Attempt to claim this trigger file,
				// by moving it to the cache directory.
				// If the move was successful, then this thread is the winner,
				// and is responsible for handling this trigger file.
				//
				Path claimedFile = null;
				try {
					claimedFile = claimTriggerFile(triggerFile);
				} catch (Exception e) {
					log.error("problem claiming trigger (" + triggerFile + " )", e);
				}
				
				if (claimedFile == null) {
					//
					// Another thread beat us to this file -- move on
					//
					continue;
				}
				else {
					//
					// We were the winner of this file.
					// Trigger file is now in the cache directory.
					//
					
					// Get partner files, by running partnerFinderImpl
					//
					List<Path> partners = findPartners(triggerFile);
					
					// "null" signifies that we expected some partners, but didn't find any.
					// An empty (non null) list signifies didn't expect partners.
					//
					if (partners == null) {
						log.info("Expected partner(s) for "+triggerFile.getFileName()+" not yet found.  Skipping...");
						
						try {
							// Not all partners found, so move to cache directory
							// so that it may serve as a partner in the future...
							//
							moveFileToDir(triggerFile, cacheDir);
						} catch (Exception e) {
							log.error("Unexpected problem moving trigger file to " + cacheDir, e);
						}
					}
					else {
						//
						// We have all of the file necessary to "GO"!
						//
						
						// Construct list of all input files (trigger file + partners)
						//
						List<Path> allInputFiles = new ArrayList<Path>();
						for (Path srcPartner : partners) {
							
							// Since partners that were found are, by definition, matched,
							// then make sure they have been moved to the cache directory
							//
							if (!srcPartner.getParent().toString().equals(cacheDir.toString())) {
								try {
									allInputFiles.add(moveFileToDir(srcPartner, cacheDir));
								} catch (Exception e) {
									log.error("Unexpected problem moving partner file to " + cacheDir, e);
								}
							}
							else {
								allInputFiles.add(srcPartner);
							}
						}
						allInputFiles.add(claimedFile); // finally add in trigger file
						
						// Now that we've claimed the trigger file
						// attempt to schedule the process
						schedule(triggerFile, claimedFile, allInputFiles);
						
					}
				}
			}
			finally {
				// Remove trigger file from list
				triggerFiles.remove(0);
			}
		}
	}
	
	
	/**
	 * Attempts to claim a trigger file, for process scheduling.
	 * If claim was not successful, then null is returned.
	 * 
	 */
	private Path claimTriggerFile(Path triggerFile) throws Exception {

		// First make sure the cache directory has not been removed by some external process,
		// otherwise we can't determine if this trigger file has been claimed
		try {
			if (!Files.exists(cacheDir)) {
				log.warn("Expected cache directory " + cacheDir.toString() + " does not exist, recreating..." );
				Files.createDirectory(cacheDir);
			}
		} catch (FileAlreadyExistsException e) {
			// Another thread probably beat us to recreating the cache directory,
			// should be safe to proceed regardless
		}

		Path instanceFile = moveFileToDir(triggerFile, cacheDir);
		if (instanceFile == null) {
			return null; // another thread beat us to this file
		}
		numMatches++;
		
		log.info("FileInitiator TRIGGERING process '"+procDefKey+"' DUE TO ARRIVAL OF "+triggerFile.toAbsolutePath() +
				" (numMatches = " + numMatches + ")");
		return instanceFile;
	}
	
	
	/**
	 * Attempts to schedule a process for a claimed trigger file.
	 * 
	 */
	private void schedule(Path triggerFile, Path instanceFile, List<Path> partners) {
		log.debug("schedule: triggerFile="+triggerFile+", instanceFile="+instanceFile+", partners="+partners.size());
		log.debug("  triggerFile exists?  " + Files.exists(triggerFile));
		log.debug("  instanceFile exists? " + Files.exists(instanceFile));
		
		try {
			
			// Now that we've claimed the trigger file
			// attempt to schedule the process.
			SchedulerJob schedulerJob = scheduleProcess(triggerFile, instanceFile, partners);
			
			if (schedulerJob != null) {
				String jobId = schedulerJob.getUuid();
				if (jobId == null) {
					throw new Exception("jobId was null!");
				}
				else {
					numInitiated++;
				}
			}
			else {
				// If we are unit testing, cwsScheduler may be null,
				// and therefore schedulerJob will be null.
				// If we aren't unit testing, and we get here, then there
				// was a problem with scheduling -- throw exception.
				if (cwsScheduler != null) {
					throw new Exception("schedulerJob was null.  Scheduling !");
				}
			}
		}
		catch (Exception e) {
			log.error("FAILED TO SCHEDULE PROCESS: " + procDefKey, e);
		}
	}
	
	
	/**
	 * Schedule the process
	 * 
	 */
	private SchedulerJob scheduleProcess(Path triggerFile, Path instanceFile, List<Path> inputFiles) throws Exception {
		// Iterate over the partners and find the matching key/pattern.
		// Add these as key/value pairs to the set of process variables.
		//
		for (Path inputFile : inputFiles) {
			String key = getMatchingPattern(inputFile.getFileName().toString());
			if (key != null) {
				// Put absolute path of each partner file under partner key
				procVariables.put(key, inputFile.toAbsolutePath().toString());
			}
		}
		
		// Add in other variables
		procVariables.put("initiatorFileName",     rootDirPath.resolve(triggerFile).getFileName().toString());
		procVariables.put("initiatorFileAbsPath",  rootDirPath.resolve(triggerFile).toAbsolutePath().toString());
		procVariables.put("initiatorFileDir",      rootDirPath.resolve(triggerFile).getParent().toString());
		procVariables.put("instanceFileName",      instanceFile.getFileName().toString());
		procVariables.put("instanceFileAbsPath",   instanceFile.toAbsolutePath().toString());
		procVariables.put("instanceFileDir",       instanceFile.getParent().toString());
		
		log.debug("About to schedule process...");
		log.trace("  with procVars: " + procVariables);
		
		if (cwsScheduler == null) {
			log.warn("not scheduling process");
			// NOT SCHEDULING PROCESS DUE TO cwsScheduler NOT LOADED.
			// THIS IS MOST LIKELY BECAUSE WE ARE TESTING OUTSIDE OF THE CONTEXT OF THE CAMUNDA PLATFORM
			// increment numInitiated here, and move file to scheduled dir, to simulate successful initiation
			numInitiated++;
			return null;
		}
		else {
			setInitiationKey(instanceFile.getFileName().toString());
			return scheduleProcess();
		}
	}
	
	
	/**
	 * Moves a file atomically to the specified directory.
	 * 
	 * @return new Path of file after move, or null if nothing found/moved
	 */
	private Path moveFileToDir(Path srcPath, Path targetDir) throws Exception {
		Path source = srcPath.toAbsolutePath();
		String targetStr = targetDir.toAbsolutePath().toString() + File.separator + srcPath.getFileName();
		Path target = new File(targetStr).toPath();
		
		log.trace("MOVING: " + source + " ---> " + target);
		Path movedFile = null;
		try {
			movedFile = Files.move(source, target, ATOMIC_MOVE);
			log.debug("MOVED: " + source + " ---> " + target);
		}
		catch (NoSuchFileException e) {
			log.debug("NoSuchFileException while moving file to directory (" +
					source + " ---> " + target + ").  This probably means another thread beat us to moving this file (" +e.getFile()+").", e);
			log.debug("source file        = " + source.toFile());
			log.debug("target file        = " + target.toFile());
			log.debug("rootDir            = " + rootDirPath);
			log.debug("targetDir          = " + targetDir);
			log.debug("source file abs    = " + source.toFile().getAbsolutePath());
			log.debug("target file abs    = " + target.toFile().getAbsolutePath());
			log.debug("source file exists = " + source.toFile().exists());
			log.debug("target file exists = " + target.toFile().exists());
			return null; // indicates no file to move
		}
		catch (Exception e) {
			log.error("Unexpected problem occurred while moving file (" +
				source + " ---> " + target + ")", e);
			throw e;
		}
		return movedFile;
	}
	
	
	/**
	 * Returns the key for the matching pattern, null otherwise.
	 * 
	 */
	private String getMatchingPattern(String test) {
		for (String key : filePatterns.keySet()) {
			String pattern = filePatterns.get(key);
			if (test.matches(pattern)) {
				return key; // found a match
			}
		}
		// If no pattern matches
		return null;
	}
	
	
	/**
	 * Returns true if at least 1 pattern matches.
	 * 
	 */
	private boolean matches(String test) {
		return (getMatchingPattern(test) != null);
	}
	
	
	/**
	 * Runs the partnerFinderImpl, to get the set of partners.
	 * The set of partners should also include the original file.
	 * 
	 */
	private List<Path> findPartners(Path triggerFile) throws IOException {
		log.debug("findParthers("+triggerFile+")...");
		
		List<Path> partners = new ArrayList<Path>();
		
		List<String> partnerStrings = null;
		if (snippetsMethod != null) {
			//
			// SNIPPETS-METHOD-BASED PARTNER FINDING
			//
			try {
				log.debug("About to invoke partner finding snippet method: " + snippetsMethod + " (" +
						triggerFile.toString() + ", " + cacheDir + ")...");
				partnerStrings = (List<String>)snippetsMethod.invoke(snippetsClassObj,
						triggerFile.toString(), rootDirStr, cacheDir.toString());
				if (partnerStrings != null) {
					for (String p : partnerStrings) {
						log.debug("  snippet found a partner: " + p);
					}
				}
				else {
					log.debug("  snippet returned null for partners");
				}
			} catch (Exception e) {
				log.error("problem invoking snippet method " + snippetsMethod + " for partner finding!", e);
				return null;
			}
		}
		else {
			//
			// CLASS IMPL BASED PARTNER FINDING
			//
			if (groupFinder == null) {
				log.error("groupFinder is null!");
				return null;
			}
			log.debug("About to invoke class-based partner finder getInputPartners method " +
					"with parameters (" + triggerFile + ", " + 
					rootDirPath + ", " + cacheDir + ")...");
			partnerStrings = groupFinder.getInputPartners(
					triggerFile.toString(), rootDirPath.toString(), cacheDir.toString());
		}
		
		// Build and return final list of Paths of partners
		//
		if (partnerStrings != null) {
			for (String partner : partnerStrings) {
				partners.add(Paths.get(partner));
			}
			return partners;
		}
		else {
			return null;
		}
	}
	
	
	//
	// SETTERS AND GETTERS
	//
	public int getPollPeriod() { return pollPeriod; }
	public void setPollPeriod(int pollPeriod) {
		this.pollPeriod = this.xtorPollPeriod = pollPeriod;
	}
	
	public String getRootDir() { return rootDirStr; }
	public void setRootDir(String rootDir) {
		this.xtorRootDirStr = this.rootDirStr = rootDir;
		
		// Setup directory paths
		//
		if (rootDir == null || rootDir.isEmpty()) {
			log.error("rootDir was null or empty!");
			return;
		}
		
		this.rootDirPath      = Paths.get(rootDir);
		this.cacheDir     = this.rootDirPath.resolve(CACHE_DIR_NAME);
		
		// Create the directories, as needed
		//
		try {
			if (!Files.isDirectory(this.rootDirPath) && !Files.exists(this.rootDirPath)) {
				// Only create root directory, if it doesn't already exist.
				log.debug("Creating directory: " + this.rootDirPath);
				Files.createDirectory(this.rootDirPath);
			}
			if (!Files.isDirectory(this.cacheDir))     { Files.createDirectory(this.cacheDir);     }
		}
		catch (Exception e) {
			log.error("There was a problem creating one or more directories for this initiator", e);
			valid = false;
			return;
		}
	}
	
	public Map<String, String> getFilePatterns() { return filePatterns; }
	public void setFilePatterns(Map<String, String> filePatterns) {
		this.filePatterns = this.xtorFilePatterns = filePatterns;
	}
	
	public String getPartnerFinderImpl() { return partnerFinderImpl; }
	public void setPartnerFinderImpl(String partnerFinderImpl) {
		this.xtorPartnerFinderImpl = partnerFinderImpl;
		if (partnerFinderImpl == null || partnerFinderImpl.isEmpty()) {
			log.info("partnerFinderImpl was null or empty!  Using default partnerFinderImpl of: " + DEFAULT_PARTNER_FINDER_IMPL);
			this.partnerFinderImpl = DEFAULT_PARTNER_FINDER_IMPL;
		}
		else {
			this.partnerFinderImpl = partnerFinderImpl;
		}
		
		if (this.partnerFinderImpl.startsWith("snippets.")) {
			// 
			try {
				urls = CodeService.urls.toArray(new URL[0]);
				
				snippetsClassObj = getCompiledClass().newInstance();
				String snippetsMethodName = this.partnerFinderImpl.substring(9);
				snippetsMethod = snippetsClassObj.getClass().getDeclaredMethod(snippetsMethodName, String.class, String.class, String.class);
			} catch (Exception e) {
				log.error("Problem locating and/or instantiating jpl.cws.core.code.Snippets class!", e);
				valid = false;
				return;
			}
		}
		else {
			// instantiate and initialize the groupFinder class
			//
			try {
				groupFinder = (GroupFinder_IF)Class.forName(this.partnerFinderImpl).newInstance();
				Map<String,Object> finderConfig = new HashMap<String,Object>();
				groupFinder.initialize(finderConfig);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				log.error("Failed to instantiate group finder plugin: " + e.getMessage());
			}
		}
	}
	
	public int getNumMatches() { return numMatches; }
	public int getNumInitiated() { return numInitiated; }
	public int getNumNonMatchingFiles() { return nonMatchingFiles.size(); }

}
