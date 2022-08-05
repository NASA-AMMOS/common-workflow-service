package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
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

			log.info("Filtering Test History Page results.");
			waitForElementID("pd-select");
			Select select = new Select(findElById("pd-select"));
			select.selectByVisibleText("Test History Page");

			waitForElementID("filter-submit-btn");

			log.info("Clicking 'Submit' button.");
			WebElement filterSubmit = findElById("filter-submit-btn");
			filterSubmit.click();

			waitForElementID("processes-table");

			log.info("Verifying the header and output from the model.");
			WebElement historyButton = findElByXPath("//button[contains(text(),'History')]");
			waitForElement(historyButton);
			scrollTo(historyButton);
			historyButton.click();

			findOnPage("<title>CWS - History</title>");

			if (findOnPage("History Page")
					&& findOnPage("Command 'mkdir Test' exit code:0.")
					&& findOnPage("Command 'ls' exit code:0.")
					&& findOnPage("LINE: Test")
					&& findOnPage("Command 'rmdir Test' exit code:0.")) {
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
