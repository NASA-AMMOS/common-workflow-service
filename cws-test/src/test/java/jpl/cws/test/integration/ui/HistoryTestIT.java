package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
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
public class HistoryTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(HistoryTestIT.class);
	private static int testCasesCompleted = 0;

	@Test
	public void runHistoryPageTest() {
		Boolean scriptPass = false;
		try {
			log.info("------ START HistoryTestIT:runResultsTest ------");
			gotoLoginPage();
			login();
			goToPage("deployments");
			startProcDef("test_history_page", "Test History Page", 90000);
			runResultsTest();
			if(Integer.toString(testCasesCompleted).equals("1")) {
				scriptPass = true;
			} else {
				log.info("Not all test cases passed. Only "+ testCasesCompleted + "/1 passed.");
			}

			log.info("------ END HistoryTestIT:runHistoryPageTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		deleteProc("test_history_page");
		logout();
		assertTrue("Deployments Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runResultsTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START HistoryTestIT:runResultsTest ------");

			goToPage("processes");

			waitForElementXPath("//input[@id=\'dt-search-0\']");

			sleep(5000);

			driver.findElement(By.xpath("//input[@id=\'dt-search-0\']")).click();
			driver.findElement(By.xpath("//input[@id=\'dt-search-0\']")).sendKeys("test_history_page");
			driver.findElement(By.xpath("//input[@id=\'dt-search-0\']")).sendKeys(Keys.ENTER);

			waitForElementID("processes-table");

			log.info("Verifying the header and output from the model.");
			
			// Try up to 3 times to interact with the history button
			int maxRetries = 3;
			boolean succeeded = false;
			
			for (int attempt = 1; attempt <= maxRetries && !succeeded; attempt++) {
				try {
					log.info("Attempt " + attempt + " to interact with history button");
					// Get fresh reference to history button
					WebElement historyButton = findElByXPath("//button[contains(text(),'History')]");
					waitForElement(historyButton);
					
					// Refresh element before scrolling
					historyButton = findElByXPath("//button[contains(text(),'History')]");
					scrollTo(historyButton);
					
					// Refresh element before clicking
					historyButton = findElByXPath("//button[contains(text(),'History')]");
					historyButton.click();
					
					succeeded = true;
					log.info("Successfully interacted with history button on attempt " + attempt);
				} catch (org.openqa.selenium.StaleElementReferenceException e) {
					if (attempt == maxRetries) {
						log.error("Failed to interact with history button after " + maxRetries + " attempts");
						throw e;
					}
					log.warn("Stale element on attempt " + attempt + ", retrying...");
					sleep(1000); // Brief pause before retry
				}
			}

			findOnPage("History");

			WebElement hideLineCheckbox = findElByXPath("//input[@id='showall']");
			waitForElement(hideLineCheckbox);

			sleep(10000);

			// Refresh element before clicking
			hideLineCheckbox = findElByXPath("//input[@id='showall']");
			hideLineCheckbox.click();

			if (findOnPage("History Page.")
					&& findOnPage("Command 'mkdir Test' exit code: 0")
					&& findOnPage("Command 'ls' exit code: 0")
					&& findOnPage("LINE: Test")
					&& findOnPage("Command 'rmdir Test' exit code: 0")) {
				scriptPass = true;
				testCasesCompleted++;
			}
			log.info("------ END HistoryTestIT:runResultsTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("HistoryTestIT-runResultsTest");
		assertTrue("Deployments Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	// Add more deployment page tests here
}
