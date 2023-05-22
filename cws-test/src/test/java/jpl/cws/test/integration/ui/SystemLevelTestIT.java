package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;


/**
 * 
 * @author hasan
 *
 */
public class SystemLevelTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(SystemLevelTestIT.class);
	private static int testCasesCompleted = 0;
	
	
	@Test
	public void runSystemLevelTest() {
		Boolean scriptPass = false;
		try {
			log.info("------ START SystemLevelTestIT:runSystemLevelTest ------");
			gotoLoginPage();
			login();
			
			runWorkerTest();
			runShutdownWorkerTest();
			
			if (Integer.toString(testCasesCompleted).equals("2")) {
				scriptPass = true;
			} else {
				log.info("Not all test cases passed. Only "+ testCasesCompleted + "/2 passed.");
			}
			logout();
			log.info("------ END SystemLevelTestIT:runSystemLevelTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		assertTrue("Deployments Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	
	public void runWorkerTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START SystemLevelTestIT:runSystemLevelTest ------");
			
			goToPage("workers");
			
			log.info("Checking if 1 worker is up, 0 are down, and none are running...");
			if (findOnPage("1 Workers (1 up, 0 down)")
					&& findOnPage("0 running")) {
				log.info("SUCCESS: 1 worker is up, 0 are down, and none are running.");
				WebElement myTable = driver.findElement(By.id("workers-table"));
				List<WebElement> myRows = myTable.findElements(By.tagName("td"));
				log.info("Getting info from Worker table...");
				log.info("Checking if the worker table has 'worker-#############'");
				if (myRows.get(0).getText().contains("worker-")) {
					log.info("SUCCESS: Found at least one worker up.");
					scriptPass = true;
					testCasesCompleted++;
				}
				
			}
			
			log.info("------ END SystemLevelTestIT:runSystemLevelTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("SystemLevelTestIT-runWorkerTest");
		assertTrue("System Level Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runShutdownWorkerTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START SystemLevelTestIT:runShutdownWorkerTest ------");
			goToPage("workers");
			
			log.info("Checking if 1 worker is up and 0 are down.");
			if (findOnPage("1 up, 0 down")) {
				log.info("SUCCESS: Found 1 worker up, 0 down.");
				
				String cwsDir = new File(System.getProperty("user.dir")).getParent();
				String stopWorkerScript = cwsDir + "/dist/worker1/cws/stop_cws.sh";
				String[] command = {"bash", "-c", "bash " + stopWorkerScript};
				log.info("Running script: 'stop_cws.sh' to stop worker.");
				
				Process proc = Runtime.getRuntime().exec(command);
				proc.waitFor();
				StringBuffer output = new StringBuffer();
	            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	            String line = "";                       
	            while ((line = reader.readLine())!= null) {
	                    output.append(line + "\n");
	            }
	            System.out.println("### " + output);
	            
	            log.info("Worker has been shutdown.");
	            
	            sleep(75000); //wait for shutdown of worker.
	            
	            driver.navigate().refresh();
	            
	            log.info("Now checking if 0 workers are up and 1 is down.");
	            if (findOnPage("0 up, 1 down")) {
	            	log.info("SUCCESS: Found 0 workers up and 1 down.");
	            	log.info("Verified the worker was shut down.");
	            	
	            	//start the worker back so it doesn't affect the rest of the test cases.
					String startWorkerScript = new File(System.getProperty("user.dir")).getParent() + "/dist/worker1/cws/start_cws.sh";
					String commandStart = "bash " + startWorkerScript;
					log.info("Starting script: 'start_cws.sh' to start the worker again.");
					
					Process procStart = Runtime.getRuntime().exec(commandStart);
					procStart.waitFor();
					StringBuffer outputStart = new StringBuffer();
		            BufferedReader readerStart = new BufferedReader(new InputStreamReader(procStart.getInputStream()));
		            String lineStart = "";                       
		            while ((lineStart = readerStart.readLine())!= null) {
		                    outputStart.append(lineStart + "\n");
		            }
		            System.out.println("### " + outputStart);
		            
		            //wait for start up of worker.
		            sleep(75000);
		            		 
		            driver.navigate().refresh();
		            
		            log.info("Checking if 1 worker is up now and 0 are down.");
		            if (findOnPage("1 up, 0 down")) {
		            	log.info("SUCCESS: 1 worker is back up and 0 are down.");
		            	scriptPass = true;
		            	testCasesCompleted++;
		            }
	            }
			}
			log.info("------ END SystemLevelTestIT:runShutdownWorkerTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("SystemLevelTestIT-runShutdownWorkerTest");
		assertTrue("Shutdown Worker Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	// Add more deployment page tests here
}
