package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 * 
 * @author hasan
 *
 */
public class ProcessesTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(ProcessesTestIT.class);
	private static int testCasesCompleted = 0;
	
	@Test
	public void runProcessesPageTest() {
		Boolean scriptPass = false;
		try {
			log.info("------ START ProcessesTestIT:runProcessesPageTest ------");
			gotoLoginPage();
			login();
			
			goToPage("deployments");
			
			startProcDef("test_processes_page", "Test Processes Page", 90000);
			
			runStatusCompleteTest();
			
			if(Integer.toString(testCasesCompleted).equals("1")) {
				scriptPass = true;
			} else {
				log.info("Not all test cases passed. Only "+ testCasesCompleted + "/1 passed.");
			}
			
			log.info("------ END ProcessesTestIT:runProcessesPageTest ------");
		} 
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		deleteProc("test_processes_page");
		logout();
		assertTrue("Processes Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
			
	public void runStatusCompleteTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		try {
			log.info("------ START ProcessesTestIT:runStatusCompleteTest ------");

			goToPage("processes");

			log.info("Getting info from table...");
			wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.tagName("table")));
			WebElement myTable = driver.findElement(By.tagName("table"));
			wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.tagName("tr")));
			List<WebElement> myRows = myTable.findElements(By.tagName("tr"));

			sleep(8000);
      
			log.info("Locating Test Processes Page from table rows and verifying that it completed.");
			waitForElementXPath("//div[@id=\'processes-table_filter\']/label/input");

			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).click();
			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).sendKeys("test_snippets_page");
			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).sendKeys(Keys.ENTER);

			waitForElementID("processes-table");
			//selenium: check if "test_processes_page" is on the page
			if (findOnPage("test_processes_page") && findOnPage("complete")) {
				log.info("Success, found proc def!");
				log.info("Found status complete for procDef");
				scriptPass = true;
				testCasesCompleted++;
			} else {
				log.info("Fail.");
				scriptPass = false;
			}
			log.info("------ END ProcessesTestIT:runStatusCompleteTest:runStatusCompleteTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("ProcessesTestIT-runStatusCompleteTest");
		assertTrue("Processes Status Complete test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

}
