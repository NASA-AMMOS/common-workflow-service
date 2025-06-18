package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 * 
 * @author hasan
 *
 */
public class WorkersTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(WorkersTestIT.class);
	private static int testCasesCompleted = 0;
	private static final String expectedTestsCompleted = String.valueOf(4);

	@Test
	public void runWorkersPageTest() {
		Boolean scriptPass = false;
		try {
			log.info("------ START WorkersTestIT:runWorkersPageTest ------");
			gotoLoginPage();
			login();
			goToPage("deployments");
			startProcDef("test_workers_page", "Test Workers Page", 0);
			runNumberActiveTest();
			runThreadLimitTest();
			runWorkersCheckBoxTest();
			runWorkersStatusTest();
			
			if (Integer.toString(testCasesCompleted).equals(expectedTestsCompleted)) {
				scriptPass = true;
				log.info("All test cases passed successfully (" + testCasesCompleted + "/" + expectedTestsCompleted + ")");
			} else {
				log.error("Test suite failed: Only " + testCasesCompleted + "/" + expectedTestsCompleted + " test cases passed");
				log.error("Failed test cases:");
				if (testCasesCompleted < 1) log.error("- Number Active Test failed");
				if (testCasesCompleted < 2) log.error("- Thread Limit Test failed"); 
				if (testCasesCompleted < 3) log.error("- Workers Checkbox Test failed");
				if (testCasesCompleted < 4) log.error("- Workers Status Test failed");
			}
			log.info("------ END WorkersTestIT:runWorkersPageTest ------");
		}
		catch (Throwable e) {
			log.error("Test suite failed with exception:", e);
			log.error("Stack trace: " + e.getMessage());
			scriptPass = false;
		}
		deleteProc("test_workers_page");
		deleteProc("test_thread_limit");
		logout();
		assertTrue("Workers Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	
	public void runWorkersCheckBoxTest() throws IOException {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		Boolean scriptPass = false;
		try {
			log.info("------ START WorkersTestIT:runWorkersCheckBoxTest ------");
			
			goToPage("deployments");
			
			log.info("Enabling worker0000 only...");
			WebElement enable = findElById("pv-test_workers_page");
			js.executeScript("arguments[0].scrollIntoViewIfNeeded();", enable);
			sleep(1000);
			// Refresh element before clicking
			enable = findElById("pv-test_workers_page");
			enable.click();
			
			log.info("Looking for worker0000 checkbox.");
			waitForElementXPath("//label[contains(text(),'worker0000')]");
			
			driver.findElement(By.xpath("//label[contains(text(),'worker0000')]")).click();
			log.info("Clicking on worker0000 checkbox.");
			waitForElementID("done-workers-btn");
			
			WebElement oneWorkerDone = findElById("done-workers-btn");
			oneWorkerDone.click();
			
			goToPage("workers");

			waitForElementXPath("//img[contains(@id,'procDefs')]");
			
			log.info("Clicking on proc defs table...");
			WebElement procDefTable = driver.findElement(By.xpath("//img[contains(@id,'procDefs')]"));
			procDefTable.click();
			
			waitForElementXPath("//input[contains(@id,'test_workers_page_enabled')]");
			
			log.info("Checking if Test Workers page was enabled.");
			WebElement checkWorkersPage = driver.findElement(By.xpath("//input[contains(@id,'test_workers_page_enabled')]"));
			
			assert(checkWorkersPage.isSelected()); //first check
			
			goToPage("processes");
			goToPage("workers");
			
			log.info("Clicking on proc defs table.");
			waitForElementXPath("//img[contains(@id,'procDefs')]");
			//need to reinitialize since js changes the img id but still keeps the same path.
			WebElement procDefTables = driver.findElement(By.xpath("//img[contains(@id,\"procDefs\")]"));
			procDefTables.click();
			sleep(1000);
			
			waitForElementXPath("//input[contains(@id,'test_workers_page_enabled')]");
			//need to reinitialize since js changes the input id but still keeps the same path
			WebElement checkWorkersPageAgain = driver.findElement(By.xpath("//input[contains(@id,\"test_workers_page_enabled\")]"));
			
			log.info("Checking if Test Workers page was enabled one more time.");
			if (checkWorkersPageAgain.isSelected()) { //second check.
				log.info("SUCCESS: Test Workers Page was enabled.");
				scriptPass = true;
				testCasesCompleted++;
				//Enable all workers again.
				goToPage("deployments");
				log.info("Disabling worker0000...");
				WebElement enableWorker = findElById("pv-test_workers_page");
				js.executeScript("arguments[0].scrollIntoViewIfNeeded();", enableWorker);
				sleep(1000);
				// Refresh element before clicking
				enableWorker = findElById("pv-test_workers_page");
				enableWorker.click();
				
				waitForElementXPath("//label[contains(text(),'worker0000')]");
				
				driver.findElement(By.xpath("//label[contains(text(),'worker0000')]")).click();
				log.info("Unchecking worker0000...");
				waitForElementID("done-workers-btn");
				
				WebElement oneWorkerDoneAgain = findElById("done-workers-btn");
				oneWorkerDoneAgain.click();
				sleep(2000);
			}
			log.info("------ END WorkersTestIT:runWorkersCheckBoxTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("WorkersTestIT-runWorkersCheckBoxTest");
		assertTrue("Workers Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	
	public void runWorkersStatusTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START WorkersTestIT:runWorkersStatusTest ------");
			
			goToPage("workers");
			log.info("Checking if worker status is up...");
			if (findOnPage("up")) { //alternative approach: can iterate through rows to find if it contains "up".
				log.info("SUCCESS: Worker status is up."); //from verifying either the header on the top of the table or in the table.
				scriptPass = true;
				testCasesCompleted++;
			}
			log.info("------ END WorkersTestIT:runWorkersStatusTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;		
		}
		screenShot("WorkersTestIT-runWorkerStatusTest");
		assertTrue("Workers Status test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runNumberActiveTest() throws IOException {
		Boolean scriptPass = false;
		try {	
			log.info("------ START WorkersTestIT:runNumberActiveTest ------");
			goToPage("workers");
			
			log.info("Checking if the worker is running...");
			if (findOnPage("1 running")) {  
				log.info("Found expected number of workers running.");
				scriptPass = true;
				testCasesCompleted++;
			}
			
			log.info("------ END WorkersTestIT:runNumberActiveTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}	
		screenShot("WorkersTestIT-runNumberActiveTest");
		assertTrue("Workers Number Active test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	
	public void runThreadLimitTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		JavascriptExecutor js = (JavascriptExecutor) driver;        
		try {
			log.info("------ START WorkersTestIT:runThreadLimitTest ------");
			
			goToPage("deployments");
			
			deployFile("test_thread_limit");
		
			enableWorkers("test_thread_limit");
			
			goToPage("workers");
			
			waitForElementXPath("//img[contains(@id,'plus_config')]");
			log.info("Opening proc configurations...");
			driver.findElement(By.xpath("//img[contains(@id,'plus_config')]")).click();
			
			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//input[contains(@id,'execThreads')]")));
			WebElement execThread = driver.findElement(By.xpath("//input[contains(@id,'execThreads')]"));
			String origThreadLimit = execThread.getAttribute("value");
			log.info("Editing thread limit to 3...");
		    execThread.clear();
		    execThread.sendKeys("3");
		    
		    waitForElementXPath("//img[contains(@id,'procDefs')]");
			
			WebElement procDefTable = driver.findElement(By.xpath("//img[contains(@id,'procDefs')]"));
			log.info("Clicking on proc def table...");
			procDefTable.click();
			
			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//input[contains(@id,'test_thread_limit_limit')]")));
			WebElement threadLimit = driver.findElement(By.xpath("//input[contains(@id,'test_thread_limit_limit')]"));
			log.info("Adjusting Test Thread Limit to 3...");
			threadLimit.clear();
			threadLimit.sendKeys("3");
			
			log.info("Clicking on workers table to apply changes...");
			driver.findElement(By.id("workers-table")).click();
			
			goToPage("initiators");
			sleep(1000);
			
			log.info("Going into Ace Editor and adding repeat initiator for test_thread_limit.");
			//go into the div element in CWS and paste it there.
			WebElement aceEditor = driver.findElement(By.cssSelector("textarea.ace_text-input"));
			js.executeScript("ace.edit('editorDiv').navigateFileEnd();");
			js.executeScript("ace.edit('editorDiv').setValue('');");
			//ToDO: upgrade to java 15 and get rid of this mess with multiline string literal
			String xmltestString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
					"<beans \n" +
					"	xmlns=\"http://www.springframework.org/schema/beans\"\n" +
					"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
					"	xsi:schemaLocation=\"\n" +
					"		http://www.springframework.org/schema/beans\n" +
					"		http://www.springframework.org/schema/beans/spring-beans.xsd\"> \n" +
					"\n" +
					"<bean id=\"repeat_2\" class=\"jpl.cws.process.initiation.RepeatingDelayInitiator\"> \n" +
					"		<property name=\"procDefKey\" value=\"test_thread_limit\" />\n" +
					"		<property name=\"delayBetweenProcesses\" value=\"0\" />\n" +
					"		<property name=\"maxRepeats\" value=\"6\" />\n" +
					"		<property name=\"procVariables\">\n" +
					"			<map>\n" +
					"				<entry key=\"variable1\" value=\"foo\"></entry>\n" +
					"				<entry key=\"variable2\" value=\"bar\"></entry>\n" +
					"			</map>\n" +
					"		</property>\n" +
					"	</bean>\n" +
					"</beans>\n" +
					"\n" +
					"\n" +
					"";

			xmltestString = xmltestString.replace("\n", "\\n").replace("\"", "\\\"");
			js.executeScript("ace.edit('editorDiv').setValue(\"" + xmltestString + "\");");

			waitForElementID("saveXmlBtn");
			log.info("Clicking on 'Save XML' button.");
			driver.findElement(By.id("saveXmlBtn")).click();
			
			waitForElementID("saveConfirmBtn");
			log.info("Clicking on 'Save Confirm' button.");
			WebElement saveConfirmBtn = driver.findElement(By.id("saveConfirmBtn"));
			
	  		js.executeScript("arguments[0].scrollIntoViewIfNeeded();", saveConfirmBtn);

			saveConfirmBtn.click();
			
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("saveConfirmBtn")));
			
			sleep(3000);
			
			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("toggle_repeat_2")));
			
			WebElement enableAction = findElById("toggle_repeat_2");
			log.info("Enabling repeat_2...");
			js.executeScript("arguments[0].click();", enableAction);
			
			goToPage("deployments");
			
			Select select = new Select(findElById("refresh-rate"));
			log.info("Adjusting refresh rate to 1 second...");
			select.selectByValue("1");
			
					
			// Each of the 3 test_thread_limit tasks is configured to sleep for 15 seconds and since they should be
			// running in parallel, we expect them all to complete after 30 seconds, but before 90 (which would be
			// the case running serially).
			sleep(80000);
			
			// Wait for element to be present and not stale
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id("stat-txt-test_thread_limit")));
			WebElement statsElement = wait.until(ExpectedConditions.refreshed(
				ExpectedConditions.presenceOfElementLocated(By.id("stat-txt-test_thread_limit"))
			));
			String child = statsElement.getText();
			log.info("Stats text after waiting: " + child);
			if (child.contains("completed: 6")) {
				log.info("Found the expected number of procs completed after 40 seconds.");
				scriptPass = true;
				testCasesCompleted++;
			}
			else {
				log.error("Did not find the expected number of completed procs (6) after 40 seconds.");
				log.error(child);
			}
			
			goToPage("workers");
			
			log.info("Reverting changes to thread limit.");
			waitForElementXPath("//img[contains(@id,'plus_config')]");
			driver.findElement(By.xpath("//img[contains(@id,'plus_config')]")).click();
			
			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//input[contains(@id,'execThreads')]")));
			
			WebElement correctThreadLimit = driver.findElement(By.xpath("//input[contains(@id,'execThreads')]"));
			
			correctThreadLimit.clear();
			correctThreadLimit.sendKeys(origThreadLimit);
			
			driver.findElement(By.id("workers-table")).click();

			log.info("------ END WorkersTestIT:runThreadLimitTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}	
		screenShot("WorkersTestIT-runThreadLimitTest");
		assertTrue("Workers Thread Limit test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	// Add more deployment page tests here
}
