package jpl.cws.test;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.process.initiation.filearrival.nio.FileInitiator;

/**
 * Unit tests related to FileInitiator
 *
 */
@RunWith(JUnit4.class)
public class FileInitiatorTest {
	
	private static final Logger log = LoggerFactory.getLogger(FileInitiatorTest.class);

	@Rule
	public ProcessEngineRule processEngineRule = new ProcessEngineRule();
	
	@Before
	public void setUp() {
		//MockitoAnnotations.initMocks(this);
	}

	@After
	public void tearDown() {
		// Delete files/directories that were created
		//
		log.info("CLEANUP watch/ignored file: "+new File("src/test/resources/watch/ignored").delete());
		log.info("CLEANUP watch/match.trigger file: "+new File("src/test/resources/watch/match.trigger").delete());
		log.info("CLEANUP watch/match2.trigger file: "+new File("src/test/resources/watch/match2.trigger").delete());
	}

	@Test
	public void dummyTest() {
		// empty test here in case other tests are commented out (as might be the case when building for Java 6
	}

	@Test
	@Deployment(resources = {"bpmn/issue_16_case1.bpmn"})
	public void testCase1_nio() throws InterruptedException {
		Map<String,String> patterns = new HashMap<String,String>();
		patterns.put("inp", ".*\\.trigger");
		FileInitiator fileInitiator = new FileInitiator();
		fileInitiator.setPollPeriod(5);
		fileInitiator.setRootDir("src/test/resources/watch");
		fileInitiator.setFilePatterns(patterns);
		fileInitiator.setPartnerFinderImpl("jpl.cws.process.initiation.filearrival.nio.SingleFinder");
		fileInitiator.setProcDefKey("issue_16_case1");
		fileInitiator.setEnabled(true);
		
		try {
			fileInitiator.start();
			Thread.sleep(100);
			
			// Create file that DOESN'T trigger
			//
			File ignoredFile = new File("src/test/resources/watch/ignored");
			ignoredFile.createNewFile();
			log.info("Created file that should be ignored.  Sleeping for 5 seconds to give initiator a chance to pick it up... " + fileInitiator.getFilePatterns());
			Thread.sleep(5000);
			if (fileInitiator.getNumMatches() != 0) {
				Assert.fail("shouldn't have matched anything yet!");
			}
			if (fileInitiator.getNumInitiated() != 0) {
				Assert.fail("shouldn't have scheduled anything yet!");
			}
			if (fileInitiator.getNumNonMatchingFiles() == 0) {
				Assert.fail("non-matching file path was not cached!");
			}
			
			// File that DOES trigger
			//
			File matchedFile = new File("src/test/resources/watch/match.trigger");
			matchedFile.createNewFile();
			log.info("Created file that should be matched (" + matchedFile.getAbsolutePath() + ").  Sleeping for 5 seconds to give initiator a chance to pick it up...");
			Thread.sleep(5000);
			if (fileInitiator.getNumMatches() != 1) {
				Assert.fail("should have matched one! (matched " + fileInitiator.getNumMatches() + ")");
			}
			if (fileInitiator.getNumInitiated() != 1) {
				Assert.fail("should have scheduled one!");
			}
			// While searching for the trigger file, the file initiator will have encountered the
			// ignored file again. It should not have been re-added to the set of non-matching files.
			if (fileInitiator.getNumNonMatchingFiles() > 1) {
				Assert.fail("non-matching file has been counted more than once!");
			}
			
			// Verify that scheduled directory was created, and contains matched file
			//
			File cacheDir = new File("src/test/resources/watch/" + FileInitiator.CACHE_DIR_NAME);
			Assert.assertTrue("expected to find match.trigger in cache directory", cacheDir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					log.info("PATH: "+pathname.getName());
					log.info(pathname.getName().endsWith("match.trigger")+"");
					return pathname.getName().endsWith("match.trigger");
				}
			}).length == 1);
			
			
			fileInitiator.setEnabled(false); // turn off
			fileInitiator.interrupt(); // stop thread
			fileInitiator = null;
		}
		catch (Throwable t) {
			Assert.fail("Failed with message: "+t.getMessage());
			log.error("error", t);
		}
		
	}
	
	
	
	@Test
	@Deployment(resources = {"bpmn/issue_16_case1.bpmn"})
	public void testMultipleThreads_nio() throws InterruptedException {
		
		Map<String,String> patterns = new HashMap<String,String>();
		patterns.put("inp", ".*\\.trigger");

		// Setup initiator 1
		//
		FileInitiator fileInitiator1 = new FileInitiator();
		fileInitiator1.setPollPeriod(3);
		fileInitiator1.setRootDir("src/test/resources/watch");
		fileInitiator1.setFilePatterns(patterns);
		fileInitiator1.setPartnerFinderImpl("jpl.cws.process.initiation.filearrival.nio.SingleFinder");
		fileInitiator1.setProcDefKey("issue_16_case1");
		fileInitiator1.setEnabled(true);
		
		// Setup initiator 2
		//
		FileInitiator fileInitiator2 = new FileInitiator();
		fileInitiator2.setPollPeriod(3);
		fileInitiator2.setRootDir("src/test/resources/watch");
		fileInitiator2.setFilePatterns(patterns);
		fileInitiator2.setPartnerFinderImpl("jpl.cws.process.initiation.filearrival.nio.SingleFinder");
		fileInitiator2.setProcDefKey("issue_16_case1");
		fileInitiator2.setEnabled(true);
		
		
		try {
			fileInitiator1.start();
			Thread.sleep(10);
			fileInitiator2.start();
			Thread.sleep(100);
			log.info("CREATED: "+fileInitiator1.getNumMatches()+", "+fileInitiator2.getNumMatches());
			
			// File that DOES trigger
			//
			File matchedFile = new File("src/test/resources/watch/match2.trigger");
			matchedFile.createNewFile();
			log.info(matchedFile.getAbsolutePath());
			log.info("Created file that should be matched.  Sleeping for 7 seconds to give initiator a chance to pick it up...");
			Thread.sleep(7000);
			
			log.info("checking results... initiator1 found " + fileInitiator1.getNumMatches() +
					", initiator2 found " + fileInitiator2.getNumMatches());
			if ((fileInitiator1.getNumMatches() + fileInitiator2.getNumMatches())  != 1) {
				Assert.fail("should have matched one! "+  fileInitiator1.getNumMatches()+" + "+fileInitiator2.getNumMatches());
			}
			if ((fileInitiator1.getNumInitiated() + fileInitiator2.getNumInitiated()) != 1) {
				Assert.fail("should have scheduled one! "+fileInitiator1.getNumInitiated()+" + "+fileInitiator2.getNumInitiated());
			}
			if ((fileInitiator1.getNumNonMatchingFiles() + fileInitiator2.getNumNonMatchingFiles()) != 0) {
				Assert.fail("matching file should not have been ignored by either initiator!");
			}
			
			
			fileInitiator1.setEnabled(false); // turn off
			fileInitiator1.interrupt(); // stop thread
			fileInitiator2.setEnabled(false); // turn off
			fileInitiator2.interrupt(); // stop thread
			fileInitiator1 = null;
			fileInitiator2 = null;
			
		}
		catch (Throwable t) {
			Assert.fail("Failed with message: "+t.getMessage());
		}
		
	}


}
