package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
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
public class LogsTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(LogsTestIT.class);
	private static int testCasesCompleted = 0;
	
	@Test
	public void runLogsPageTest() {
		Boolean scriptPass = false;
		
		try {
			log.info("------ START LogsTestIT:runLogPageTest ------");
			gotoLoginPage();
			login();
			
			goToPage("deployments");
			
			startProcDef("test_logs_page", "Test Logs Page", 70000);
			
			runOutputTest();
			runTableColumnTest();
			//runOutputRefreshTest();
			
			if (Integer.toString(testCasesCompleted).equals("2")) {
				scriptPass = true;
			} else {
				log.info("Not all test cases passed. Only "+ testCasesCompleted + "/3 passed.");
			}
			
			log.info("------ END LogsTestIT:runLogsPageTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;	
		}
		deleteProc("test_logs_page");
		// deleteProc("output_refresh_test");
		logout();
		assertTrue("Logs Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	
	public void runOutputTest() throws IOException {
		Boolean scriptPass = false;
		
		try {
			log.info("------ START LogsTestIT:runOutputTest ------");

			goToPage("logs");
			
			log.info("Looking for text, 'Graphite', 'Command ls exit exit code: 0', and 'Deployed process definitions: test_logs_page.bpmn'.");

			if (findOnPage("Graphite")
					&& findOnPage("Command 'ls' exit code: 0")
					&& findOnPage("Deployed process definition: 'test_logs_page.bpmn'")) {
				scriptPass = true;
				testCasesCompleted++;
			} else {
				log.info("Not found.");
			}
			log.info("------ END LogsTestIT:runOutputTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;	
		}
		screenShot("LogsTestIT-runOutputTest");
		assertTrue("Logs Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	
	public void runTableColumnTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver,30);
		try {
			log.info("------ START LogsTestIT:runTableColumnTest ------");
			
			goToPage("logs");
			
			waitForElementID("cwshost-chkbox");
			log.info("Checking CWS Host.");
			findElById("cwshost-chkbox").click();
			sleep(1000);
			
			waitForElementID("cwswid-chkbox");
			log.info("Checking CWS ID.");
			findElById("cwswid-chkbox").click();
			sleep(1000);
			
			waitForElementID("loglvl-chkbox");
			log.info("Checking Log Level.");
			findElById("loglvl-chkbox").click();
			sleep(1000);
			
			waitForElementID("thn-chkbox");
			log.info("Checking Thread Name.");
			findElById("thn-chkbox").click();
			sleep(1000);
			
			waitForElementID("pdk-chkbox");
			log.info("Checking Process Definition Key.");
			findElById("pdk-chkbox").click();
			sleep(1000);
			
			waitForElementID("pid-chkbox");
			log.info("Checking Process ID.");
			findElById("pid-chkbox").click();
			sleep(1000);
			
			wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.tagName("table")));
			WebElement myTable = driver.findElement(By.tagName("table"));
			
			log.info("Checking data from table to ensure all checkboxes were clicked: " + myTable.getText());
			
			if (myTable.getText().contains("Time Stamp CWS Host CWS Worker ID Log Level Thread Name Proc Def Key Proc Inst ID Message")) {
				scriptPass = true;
				log.info("All checkboxes were successfully checked.");
				testCasesCompleted++;
			}

			log.info("------ END LogsTestIT:runTableColumnTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;	
		}
		screenShot("LogsTestIT-runTableColumnTest");
		assertTrue("Table Column Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	
	//Disabled test for inconsistency on the refresh in the logs.
	public void runOutputRefreshTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver,30);
		try {
			log.info("------ START LogsTestIT:runOutputRefreshTest ------");
			
			goToPage("deployments");
			
			deployFile("output_refresh_test");
			
			wait.until(ExpectedConditions.elementToBeClickable(By.id("pv-output_refresh_test")));
			WebElement enable = findElById("pv-output_refresh_test");
			enable.click();
			sleep(1000);
			
			WebElement allWorkers = findElById("all-workers");
			WebElement allWorkersDone = findElById("done-workers-btn");
			
			
			if (allWorkers.isSelected()) {
				allWorkersDone.click();
				sleep(1000);
			} else {
				allWorkers.click();
				sleep(1000);
				allWorkersDone.click();
				sleep(1000);
			}
			
			sleep(2000);
			
			wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='/camunda/app/tasklist']")));
			WebElement tasks = driver.findElement(By.xpath("//a[@href='/camunda/app/tasklist']"));
			tasks.click();		
			findOnPage("Camunda Tasklist");
			
			waitForElementXPath("//*[contains(@class,'start-process-action')]");
			
			WebElement start = driver.findElement(By.xpath("//*[contains(@class,'start-process-action')]"));
			start.click();
			
			waitForElementXPath("//input[@placeholder='Search by process name.']");
			
			WebElement search = driver.findElement(By.xpath("//input[@placeholder='Search by process name.']"));
			search.click();
			search.sendKeys("Output Refresh Test");
			
			waitForElementXPath("//*[contains(text(), 'Output Refresh Test')]");
			
			WebElement li = driver.findElement(By.xpath("//*[contains(text(), 'Output Refresh Test')]"));
			li.click();
			
			waitForElementXPath("//button[contains(text(),'Start')]");
			
			WebElement button = driver.findElement(By.xpath("//button[contains(text(),'Start')]"));
			button.click();
			
			goToPage("logs");
			
			sleep(3000);
			waitForElementID("refresh-rate");
			Select select = new Select(findElById("refresh-rate"));
			select.selectByVisibleText("1 second");
			
			waitForElementID("refresh-checkbox");
			findElById("refresh-checkbox").click();
			sleep(3000);
			
			int logMessageCounter = 0;
			if (findOnPage("1 second log")) {
				logMessageCounter++;
			}
			sleep(5000);
			
			if (findOnPage("5 second log")) {
				logMessageCounter++;
			}
			
			sleep(10000);
			
			if (findOnPage("10 second log")) {
				logMessageCounter++;
			}
			
			//make sure expected results match actual.
			if (Integer.toString(logMessageCounter).equals("3")) {
				scriptPass = true;
				testCasesCompleted++;
				procCounter++;
			}
			
			log.info("------ END LogsTestIT:runOutputRefreshTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;	
		}
		screenShot("LogsTestIT-runOutputRefreshTest");
		assertTrue("Output Refresh Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	// Add more deployment page tests here
}
