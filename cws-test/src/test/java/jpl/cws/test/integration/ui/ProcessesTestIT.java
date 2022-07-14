package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
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
		WebDriverWait wait = new WebDriverWait(driver,30);
		try {
			log.info("------ START ProcessesTestIT:runStatusCompleteTest ------");

			goToPage("processes");
			
			log.info("Getting info from table...");
			wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.tagName("table")));
			WebElement myTable = driver.findElement(By.tagName("table"));
			wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.tagName("tr")));
			List<WebElement> myRows = myTable.findElements(By.tagName("tr"));
			
			log.info("Locating Test Processes Page from table rows and verifying that it completed.");
			for (int i = 0; i < myRows.size(); i++) {
				String row  = myRows.get(i).getText();
				log.info(row);
				
				if (row.contains("test_processes_page")) {
					log.info("Success, found proc def!");
					
					wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("id('processes-table')/tbody/tr[\"+i+\"]")));
					//Looking at row index for test_proccesses_page and checking for a complete status
					String status = driver.findElement(By.xpath("id('processes-table')/tbody/tr["+i+"]")).getText();
					
					if (status.contains("complete")) {
						log.info("Found status complete for procDef");
						scriptPass = true;
						testCasesCompleted++;
						break;
					} else {
						log.info("Fail.");
						scriptPass = false;
					}
				}
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
