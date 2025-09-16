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

			waitForElementXPath("//input[@id=\'dt-search-1\']");

			sleep(5000);

			driver.findElement(By.xpath("//input[@id=\'dt-search-1\']")).click();
			driver.findElement(By.xpath("//input[@id=\'dt-search-1\']")).sendKeys("test_history_page");
			driver.findElement(By.xpath("//input[@id=\'dt-search-1\']")).sendKeys(Keys.ENTER);

			waitForElementID("processes-table");

			log.info("Verifying the header and output from the model.");
			
			// Wait for process to complete and history button to be enabled
			sleep(3000);
			waitForElementXPath("//button[contains(text(),'History') and not(contains(@class, 'disabled'))]");
			
			// Find and click the history link
			try {
				// Find the anchor tag with history link
				WebElement historyLink = findElByXPath("//a[contains(@href, 'history?procInstId')]");
				String href = historyLink.getAttribute("href");
				log.info("Found history link with href: " + href);
				
				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("arguments[0].scrollIntoViewIfNeeded();", historyLink);
				sleep(500);
				
				// Try regular click first, then JavaScript click if needed
				try {
					historyLink.click();
				} catch (Exception e) {
					log.info("Regular click intercepted, using JavaScript click");
					js.executeScript("arguments[0].click();", historyLink);
				}
				
				log.info("Successfully clicked history link");
			} catch (Exception e) {
				log.error("Failed to click history link: " + e.getMessage());
				// Try fallback with button
				WebElement historyButton = findElByXPath("//button[contains(text(),'History')]");
				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("arguments[0].click();", historyButton);
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
