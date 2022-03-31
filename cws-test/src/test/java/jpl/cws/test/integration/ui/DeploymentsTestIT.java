package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.python.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author hasan
 *
 */
@Ignore
@RunWith(JUnit4.class)
public class DeploymentsTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(DeploymentsTestIT.class);
	private static int completedCount = 0; //tracker for completed Deployments proc.
	private static int testCasesCompleted = 0;

	@Test
	public void DeploymentsPageTest() {
		Boolean scriptPass = false;
		try {
			log.info("------ START DeploymentsTestIT:runDeploymentsPageTest ------");
			gotoLoginPage();
			login();

			processCompletedTest();
			runEnableDisableButtonTest();
			runDeployFileTest();
	//		runBrowseFileTest();
			runFileVersionTest();
			runTotalStatusTest();
			runStatusRefreshTest();
			runOneWorkerTest();
			runDeleteProcTest();

			//Verify that all test cases passed successfully.
			if (Integer.toString(testCasesCompleted).equals("8")) {
				scriptPass = true;
			} else {
				log.info("Not all test cases passed. Only " + testCasesCompleted + "/9 passed.");
			}

			log.info("------ END DeploymentsTestIT:runDeploymentsPageTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		deleteProc("test_hello_world");
		deleteProc("test_deployments_page");
		logout();
		assertTrue("Deployments Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void processCompletedTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START DeploymentsTestIT:runProcessCompletedTest ------");
			// Fill in more here

			goToPage("deployments");
			startProcDef("test_deployments_page", "Test Deploy Page");
			completedCount++;

			WebElement statsText = driver.findElement(By.id("stat-txt-test_deployments_page"));
			log.info("Getting text from Status Bar of Test Deploy Page.");
			String child = statsText.getText();
			log.info(child);

			//analyze string to check how many procs completed.
			if (child.contains("completed: "+completedCount)) {
				String color = driver.findElement(By.className("progress-bar-success")).getCssValue("background-color");
				log.info(color);

				if (color.equals("rgba(92, 184, 92, 1)")) { //color = green
					scriptPass = true;
					testCasesCompleted++;
				}
			}

			log.info("------ END DeploymentsTestIT:runProcessCompletedTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT::runProcessCompletedTest");
		assertTrue("Deployments Process Completed test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runEnableDisableButtonTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver,30);
		try {
			log.info("------ START DeploymentsTestIT:runEnableDisableButtonTest ------");

			WebElement enable = findElById("pv-test_deployments_page");
			WebElement allWorkers = findElById("all-workers");
			WebElement allWorkersDone = findElById("done-workers-btn");

			log.info("Checking if workers are enabled already,");
			if (enable.getText().equals("enable")) {
				log.info("Workers are not enabled. Enabling workers now.");
				enable.click();
				waitForElement(allWorkersDone);
				if (allWorkers.isSelected()) {
					allWorkersDone.click();
				} else {
					allWorkers.click();
					waitForElement(allWorkersDone);
					allWorkersDone.click();
				}
				if (enable.getText().equals("view")) {
					log.info("Workers are enabled.");
					scriptPass = true;
					testCasesCompleted++;
				}
			} else if (enable.getText().equals("view")) {
				log.info("Workers are already enabled.");
				scriptPass = true;
				testCasesCompleted++;
			} else {
				log.info("We found this text instead on the enable button: "+ enable.getText());
			}
			wait.until(ExpectedConditions.invisibilityOf(allWorkersDone));
			log.info("------ END DeploymentsTestIT:runEnableDisableButtonTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT:runEnableDisableButtonTest");
		assertTrue("Deployments Enable/Disable Button test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runDeployFileTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START DeploymentsTestIT:runDeployFileTest ------");

			deployFile("test_deployments_page");
			findOnPage("Deployed process definition: test_deployments_page.bpmn.");

			String procNameCheck = driver.findElement(By.xpath("//a[contains(text(),'Test Deploy Page')]")).getText();
			if (procNameCheck.equals("Test Deploy Page")) {
				log.info("File deployed successfully!");
				scriptPass = true;
				testCasesCompleted++;
			}
			log.info("------ END DeploymentsTestIT:runDeployFileTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT:runDeployFileTest");
		assertTrue("Deployments Deploy file test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runBrowseFileTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START DeploymentsTestIT:runBrowseFileTest ------");

			WebElement fileUploadInput = findElById("file-input");

			fileUploadInput.sendKeys(TEST_BPMN_DIR+"/test_deployments_page.bpmn");

			String procTextCheck = driver.findElement(By.xpath("//label[contains(text(),'test_deployments_page.bpmn')]")).getText();

			if (procTextCheck.equals("test_deployments_page.bpmn")) {
				log.info("Found the Process text that was browsed.");
				scriptPass = true;
				testCasesCompleted++;
			}
			log.info("------ END DeploymentsTestIT:runBrowseFileTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT:runBrowseFileTest");
		assertTrue("Deployments Browse file test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runFileVersionTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START DeploymentsTestIT:runFileVersionTest ------");

			//Deploying identical file from temp directory.
			WebElement fileUploadInput = findElById("file-input");

			log.info("Deploying version 1 of test_deployments_page.bpmn");
			String fileName  = "test_deployments_page";
			log.info("Creating temp directory for version 2 of Test Deploy Page.");
			File tempDir = Files.createTempDir();
			String sourcePath = TEST_BPMN_DIR+"/" + fileName + ".bpmn";
			String destPath = tempDir.toPath().toString() + "/" + fileName + ".bpmn";

			//for copyFile() method.
			File source = new File(sourcePath);
	        File dest = new File(destPath);

	        log.info("Copying Version 1 contents into temp directory to be modified.");
			copyFile(source, dest); //copies file to tmp dir.

			modifyFile(destPath, "Deployments test.", "Deployments EDITED!"); //changes the content of the duplicate.
			log.info("Modified text of version 1 from 'Deployments test' to 'Deployments EDITED!'.");
			fileUploadInput.sendKeys(destPath);
			log.info("Uploaded modified version.");

			findOnPage("Deployed Process Definitions");
			findOnPage(fileName);

			waitForElementID("process-table");
			WebElement myTable = driver.findElement(By.id("process-table"));
			List<WebElement> myRows = myTable.findElements(By.tagName("tr"));
			log.info("Checking if file version changed to version 2.");
			for (int i = 0; i < myRows.size(); i++) {
				String row  = myRows.get(i).getText();
				log.info(row);

				if (row.contains("Test Deploy Page")) {
					String status = driver.findElement(By.xpath("id('process-table')/tbody/tr["+i+"]")).getText();
					log.info(status);

					if (status.contains("2")) { //2 = second version
						log.info("Found new and updated version.");
						scriptPass = true;
						testCasesCompleted++;
						log.info("Deleting temp directory that was created.");
						tempDir.deleteOnExit(); //deletes the tmp dir created.
						break;
					} else {
						log.info("Fail.");
						scriptPass = false;
					}
				}
			}
			log.info("------ END DeploymentsTestIT:runFileVersionTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT:runFileVersionTest");
		assertTrue("Deployment File Version test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runDeleteProcTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver,30);
		try {
			log.info("------ START DeploymentsTestIT:runDeleteProcTest ------");

			goToPage("deployments");

			log.info("Disabling workers.");
			wait.until(ExpectedConditions.elementToBeClickable(By.id("pv-test_deployments_page")));
			WebElement enable = findElById("pv-test_deployments_page");
			enable.click();
			sleep(1000);

			WebElement allWorkers = findElById("all-workers");
			WebElement allWorkersDone = findElById("done-workers-btn");

			if(allWorkers.isSelected()) {
				allWorkers.click();
				sleep(1000);
				allWorkersDone.click();
				sleep(1000);
			} else {
				allWorkersDone.click();
				sleep(1000);
			}

			log.info("Deleting process definition");
			wait.until(ExpectedConditions.elementToBeClickable(By.id("delete-test_deployments_page")));
			WebElement delButton = driver.findElement(By.id("delete-test_deployments_page"));
			delButton.click();


			waitForElementID("delete-proc-def");
			findElById("delete-proc-def").click();

			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("delete-proc-def")));

			driver.navigate().refresh();

			if (driver.getPageSource().contains("Test Deploy Page")) {
				log.info("Did not delete all instances of proc.");
				scriptPass = false;
			} else {
				log.info("Successfully deleted all instances of proc.");
				scriptPass = true;
				testCasesCompleted++;
				procCounter  = procCounter - 1;
			}
			log.info("------ END DeploymentsTestIT:runDeleteProcTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT:runDeleteProcTest");
		assertTrue("Deployment Delete Proc test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runTotalStatusTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START DeploymentsTestIT:runTotalStatusTest ------");

			startProcDef("test_hello_world", "Test Hello World"); //no need to increment completedCount for HelloWorld.
			completedCount++;
			WebElement statsText = driver.findElement(By.id("stat-txt-cws-reserved-total"));
			String child = statsText.getText();
			log.info(child);

			log.info("Verifying that expected completed count matches actual completed count.");
			if (child.contains("completed: "+completedCount )) {
				log.info("Found a total of: "+ completedCount  + " procs. Expected completed count: " + completedCount);
				scriptPass = true;
				testCasesCompleted++;
			}
			log.info("------ END DeploymentsTestIT:runTotalStatusTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT:runTotalStatusTest");
		assertTrue("Deployment Total Status test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runStatusRefreshTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START DeploymentsTestIT:runStatusRefreshTest ------");

			log.info("Changing refresh second to 1.");
			Select select = new Select(findElById("refresh-rate"));
			select.selectByVisibleText("1 second refresh rate");

			if (driver.getPageSource().contains("1 second refresh rate")) {
				log.info("Found the seledcted Status Refresh Rate.");
				scriptPass = true;
				testCasesCompleted++;
			}

			log.info("------ END DeploymentsTestIT:runStatusRefreshTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT:runStatusRefreshTest");
		assertTrue("Deployment Status Refresh test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runOneWorkerTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver,30);
		try {
			log.info("------ START DeploymentsTestIT:runOneWorkerTest ------");
			WebElement enable = findElById("pv-test_deployments_page");
			waitForElement(enable);
			enable.click();

			log.info("Enabling one worker.");
			waitForElementXPath("//label[contains(text(),'worker0001')]");
			WebElement worker0001 = driver.findElement(By.xpath("//label[contains(text(),'worker0001')]"));
			worker0001.click();

			WebElement oneWorkerDone = findElById("done-workers-btn");
			waitForElement(oneWorkerDone);
			oneWorkerDone.click();

			wait.until(ExpectedConditions.invisibilityOf(oneWorkerDone));

			wait.until(ExpectedConditions.elementToBeClickable(enable));

			enable.click();

			waitForElement(oneWorkerDone);

			List<WebElement> selectElements = driver.findElements(By.cssSelector("input[class='worker-checkbox']"));

			log.info("Verifying one worker is selected.");
			for (WebElement checkbox : selectElements){
			    //iterate over both check boxes to make sure only one is selected by the # of logs printed.
				if(checkbox.isSelected()){
			      log.info("Found selected checkbox."+ selectElements.toString());
			      scriptPass = true;
			      testCasesCompleted++;
			    }
			}

			driver.findElement(By.xpath("//label[contains(text(),'worker0001')]")).click();
			sleep(1000);
		    oneWorkerDone.click();
		    wait.until(ExpectedConditions.invisibilityOf(oneWorkerDone));
			log.info("------ END DeploymentsTestIT:runOneWorkerTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("DeploymentsTestIT:runOneWorkerTest");
		assertTrue("Deployment One Worker test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	// Add more deployment page tests here
}
