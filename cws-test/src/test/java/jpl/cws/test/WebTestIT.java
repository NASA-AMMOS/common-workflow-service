package jpl.cws.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ghollins
 *
 */
public class WebTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(WebTestIT.class);
	
	
	
	@Test
	public void testGoogleSearch() throws InterruptedException, IOException {
	  
	  driver.get("http://www.google.com");
	  
	  Thread.sleep(5000);  // Let the user actually see something!
	  WebElement searchBox = driver.findElement(By.name("q"));
	  searchBox.sendKeys("ChromeDriver");
	  searchBox.submit();
	  Thread.sleep(5000);  // Let the user actually see something!
	  
	  //log.info(driver.getPageSource());
	  
	  File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		// Now you can do whatever you need to do with it, for example copy somewhere
		FileUtils.copyFile(scrFile, new File("/tmp/screenshot.png"));
	}
	
	@Test
	public void loginTest() {
		log.info("------ START loginTest ------");
		gotoLoginPage();
		login();
		logout();
		log.info("------ END loginTest ------");
	}
	
	@Test
	public void deployTest() {
		log.info("------ START deployTest ------");
		gotoLoginPage();
		login();
		gotoDeployments();
		uploadProcessDefinitionFile();
		logout();
		log.info("------ END deployTest ------");
	}
	
	@Test
	public void runDeployTest() {
		log.info("------ START deployTest ------");
		gotoLoginPage();
		login();
		gotoDeployments();
		uploadProcessDefinitionFile();
		
		// Enable Process Def
		WebElement enable = findElById("pv-test_set_vars");
		enable.click();
		sleep(1000);
		
		WebElement allWorkers = findElById("all-workers");
		allWorkers.click();
		sleep(1000);
		
		WebElement allWorkersDone = findElById("done-workers-btn");
		allWorkersDone.click();
		sleep(10000);
		
		
		// Start Instance
		WebElement tasks = driver.findElement(By.xpath("//a[@href='/camunda/app/tasklist']"));
		tasks.click();		
		findOnPage("<title>Camunda Tasklist</title>");
		
		sleep(10000);
		
		WebElement start = driver.findElement(By.xpath("//*[contains(@class,'start-process-action')]"));
		start.click();
		sleep(5000);

		WebElement searchProcessField = driver.findElement(By.xpath("//input[contains(@class,'form-control')]"));
		searchProcessField.sendKeys("Test Set Variables");
		sleep(5000);

		WebElement li = driver.findElement(By.xpath("//*[contains(text(),'Test Set Variables')]"));
		li.click();
		sleep(5000);
		
		WebElement button = driver.findElement(By.xpath("//button[contains(text(),'Start')]"));
		button.click();
		sleep(5000);

		
		// Go back to CWS
		WebElement cws = driver.findElement(By.xpath("//a[@href='/cws-ui']"));
		cws.click();
		findOnPage("<title>CWS - Deployments</title>");
		

		// gotoDeployments();
		
		// Wait for Finish
		sleep(25000);
		
		logout();
		log.info("------ END deployTest ------");
	}

	@Test
	public void runErrorHandlingTest() {
		log.info("------ START deployTest ------");
		gotoLoginPage();
		login();
		gotoDeployments();
		uploadErrorHandlingProcessDefinitionFile();

		
		// Enable Process Def
		WebElement enable = findElById("pv-test_error_handling");
		enable.click();
		sleep(1000);

		
		WebElement allWorkers = findElById("all-workers");
		allWorkers.click();
		sleep(1000);
		
		WebElement allWorkersDone = findElById("done-workers-btn");
		allWorkersDone.click();
		sleep(10000);
		
		
		// Start Instance (1) through Camunda
		WebElement tasks = driver.findElement(By.xpath("//a[@href='/camunda/app/tasklist']"));
		tasks.click();		
		findOnPage("<title>Camunda Tasklist</title>");
		
		sleep(10000);
		
		WebElement start = driver.findElement(By.xpath("//*[contains(@class,'start-process-action')]"));
		start.click();
		sleep(5000);

		WebElement searchProcessField = driver.findElement(By.xpath("//input[contains(@class,'form-control')]"));
		searchProcessField.sendKeys("Test Error Handling");
		sleep(5000);

		WebElement li = driver.findElement(By.xpath("//*[contains(text(),'Test Error Handling')]"));
		li.click();
		sleep(5000);
		
		WebElement button = driver.findElement(By.xpath("//button[contains(text(),'Start')]"));
		button.click();
		sleep(5000);

		
		// Go back to CWS
		WebElement cws = driver.findElement(By.xpath("//a[@href='/cws-ui']"));
		cws.click();
		findOnPage("<title>CWS - Deployments</title>");
		
		//gotoDeployments();
	
		// Wait for Finish
		sleep(90000);
		procCounter++;
		
		logout();
		log.info("------ END deployTest ------");
	}
	
	@Test
	public void runHelloWorldTest() {
		log.info("------ START deployTest ------");
		gotoLoginPage();
		login();
		goToPage("deployments");

		uploadTestHelloWorld();
		
		WebElement enable = findElById("pv-test_hello_world");
		enable.click();
		sleep(1000);
		
		WebElement allWorkers = findElById("all-workers");
		WebElement allWorkersDone = findElById("done-workers-btn");
		
		if(allWorkers.isEnabled()) {
			allWorkersDone.click();
			sleep(10000);
		} else {
			allWorkers.click();
			sleep(1000);
			allWorkersDone.click();
			sleep(10000);
		}
		
		WebElement tasks = driver.findElement(By.xpath("//a[@href='/camunda/app/tasklist']"));
		tasks.click();		
		findOnPage("<title>Camunda Tasklist</title>");
		
		sleep(10000);
		
		WebElement start = driver.findElement(By.xpath("//*[contains(@class,'start-process-action')]"));
		start.click();
		sleep(5000);

		WebElement searchProcessField = driver.findElement(By.xpath("//input[contains(@class,'form-control')]"));
		searchProcessField.sendKeys("Test Hello World");
		sleep(5000);

		WebElement li = driver.findElement(By.xpath("//*[contains(text(),'Test Hello World')]"));
		li.click();
		sleep(5000);
		
		WebElement button = driver.findElement(By.xpath("//button[contains(text(),'Start')]"));
		button.click();
		sleep(5000);

		
		// Go back to CWS
		WebElement cws = driver.findElement(By.xpath("//a[@href='/cws-ui']"));
		cws.click();
		findOnPage("<title>CWS - Deployments</title>");
		
		
		/*
		//Start Instance (2) through Initiators
		goToInitiators();
		
		sleep(9000000);
		
		WebElement saveXmlBtn = findElById("saveXmlBtn");
		saveXmlBtn.click();
		sleep(1000);
		
		WebElement saveConfirmBtn = findElById("saveConfirmBtn");
		saveConfirmBtn.click();
		sleep(1000);
		allWorkers.click();
		sleep(1000);
		allWorkersDone.click();
		sleep(1000);
		
		WebElement enableAction = findElById("toggle_repeat_1");
		enableAction.click();
		sleep(1000);
		
		gotoDeployments();
		
		sleep(5000);
		
		goToProcesses();
		
		sleep(1000);
		*/
		
		// goToPage("deployments");
		
		
		if(findOnPage("completed")) {
			goToProcesses();
			sleep(1000);
			log.info("Found a completed task.");
			
			WebElement completeBox = findElById("complete");
			completeBox.click();
			sleep(1000);
			
			Select select = new Select(findElById("pd-select"));
			select.selectByVisibleText("Test Hello World");
			sleep(1000);
			
			WebElement filterSubmit = findElById("filter-submit-btn");
			filterSubmit.click();
			sleep(1000);

			WebElement historyButton = driver.findElement(By.xpath("//button[contains(text(),'History')]"));
			historyButton.click();
			sleep(5000);
			
			findOnPage("ls");
			findOnPage("Hello World");
			findOnPage("Desktop");
			findOnPage("Applications");
			
			sleep(9000);
			
			
		} else {
			log.info("Process did not complete either in time or at all");
		}
		
		logout();
		log.info("------ END deployTest ------");
	}
	@Test
	public void runProcessTest() {
		log.info("------ START runProcessTest ------");
		gotoLoginPage();
		login();
		startProcessFromConsole("test_set_vars");
		logout();
		log.info("------ END runProcessTest ------");
	}
	
	
	
	private void uploadProcessDefinitionFile() {
//		WebElement fileUploadInput = findElById("file-input");
//		
//		fileUploadInput.sendKeys(TEST_BPMN_DIR+"/test_set_vars.bpmn");
//		
//		WebElement deployProcDefBtn = findElById("deployProcDefBtn");
//		deployProcDefBtn.click();
//		findOnPage("Deployed Process Definitions");
//		findOnPage("test_set_vars");
//		
//		sleep(1000);
		deployFile("test_set_vars");
	}
	
	private void uploadErrorHandlingProcessDefinitionFile() {
//		WebElement fileUploadInput = findElById("file-input");
//		
//		fileUploadInput.sendKeys(TEST_BPMN_DIR+"/test_error_handling.bpmn");
//		
//		WebElement deployProcDefBtn = findElById("deployProcDefBtn");
//		deployProcDefBtn.click();
//		findOnPage("Deployed Process Definitions");
//		findOnPage("test_error_handling");
//		
//		sleep(1000);
		deployFile("test_error_handling");
		
	}
		
	private void startProcessFromConsole(String procDefKey) {
		findOnPage(procDefKey);
		WebElement startIcon = findElById("pv-test_set_vars");
		log.info("Clicking on " + startIcon);
		startIcon.click();
		findOnPage("numPending_test_set_vars");
	}
	
	//Demo for Sarjil
	private void goToProcesses() {
		if(findOnPage("completed")) {
			log.info("Found completed tasks.");
			driver.get("http://"+HOSTNAME+":"+PORT + "/cws-ui/processes");
		} else {
			log.info("Found no completed tasks.");
		}
	}
	
	private void goToInitiators() {
		log.info("Navigating to Initiators page");
		driver.get("http://"+HOSTNAME+":"+PORT + "/cws-ui/initiators");
	}
	private void uploadExternalPrintWorkingDirectory() {
		deployFile("external_pwd");
	}
	
	private void uploadTestHelloWorld() {
		deployFile("test_hello_world");
	}
	
	//---End Demo---
	
}
