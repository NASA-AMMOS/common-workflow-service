package jpl.cws.test.integration.ui;

import java.io.File;
import java.io.IOException;

import jpl.cws.test.WebTestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openqa.selenium.*;
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
		deployFile("test_set_vars");
		deleteProc("test_set_vars");
		logout();
		log.info("------ END deployTest ------");
	}

	@Test
	public void runDeployTest() {
		log.info("------ START deployTest ------");
		gotoLoginPage();
		login();
		gotoDeployments();
		deployFile("test_set_vars");

		// Enable Process Def
		enableWorkers("test_set_vars");


		// Start Instance
		WebElement tasks = driver.findElement(By.xpath("//a[@href='/camunda/app/tasklist']"));
		tasks.click();
		findOnPage("Camunda Tasklist");

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
		findOnPage("CWS - Deployments");


		// Wait for Finish
		sleep(90000);

		deleteProc("test_set_vars");
		logout();
		log.info("------ END deployTest ------");
	}

	@Test
	public void runErrorHandlingTest() {
		log.info("------ START deployTest ------");
		gotoLoginPage();
		login();
		gotoDeployments();
		deployFile("test_error_handling");

		// Enable Process Def
		enableWorkers("test_error_handling");


		// Start Instance (1) through Camunda
		WebElement tasks = driver.findElement(By.xpath("//a[@href='/camunda/app/tasklist']"));
		tasks.click();
		findOnPage("Camunda Tasklist");

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
		findOnPage("CWS - Deployments");

		// Wait for Finish
		sleep(180000);
		procCounter++;

		deleteProc("test_error_handling");
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

		enableWorkers("test_hello_world");

		WebElement tasks = driver.findElement(By.xpath("//a[@href='/camunda/app/tasklist']"));
		tasks.click();
		findOnPage("Camunda Tasklist");

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
		findOnPage("CWS - Deployments");

		// Wait for Finish
		sleep(90000);

		if(findOnPage("completed")) {
			goToProcesses();
			sleep(1000);
			log.info("Found a completed task.");

			waitForElementXPath("//div[@id=\'processes-table_filter\']/label/input");

			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).click();
			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).sendKeys("test_hello_world");
			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).sendKeys(Keys.ENTER);

			waitForElementID("processes-table");

			waitForElementXPath("//a[contains(text(),'History')]");
			WebElement historyButton = driver.findElement(By.xpath("//a[contains(text(),'History')]"));
			historyButton.click();
			sleep(1000);

			findOnPage("ls");
			findOnPage("Hello World");
			findOnPage("Command 'ls' exit code: 0");

			sleep(9000);


		} else {
			log.info("Process did not complete either in time or at all");
		}

		deleteProc("test_hello_world");
		logout();
		log.info("------ END deployTest ------");
	}

	@Test
	public void runGroovyTest() {
		log.info("------ START runGroovyTest ------");
		gotoLoginPage();
		login();
		goToPage("deployments");

		startProcDef("test_groovy_script", "Test Groovy Script", 90000);

		if(findOnPage("completed")) {
			goToProcesses();
			sleep(1000);
			log.info("Found a completed task.");

			waitForElementXPath("//div[@id=\'processes-table_filter\']/label/input");

			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).click();
			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).sendKeys("test_groovy_script");
			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).sendKeys(Keys.ENTER);

			waitForElementID("processes-table");

			waitForElementXPath("//a[contains(text(),'History')]");
			WebElement historyButton = driver.findElement(By.xpath("//a[contains(text(),'History')]"));
			historyButton.click();
			sleep(1000);

			findOnPage("Groovy.");

			sleep(9000);


		} else {
			log.info("Process did not complete either in time or at all");
		}

		deleteProc("test_groovy_script");
		logout();
		log.info("------ END runGroovyTest ------");
	}

	@Test
	public void runProcessTest() {
		log.info("------ START runProcessTest ------");
		gotoLoginPage();
		login();
		startProcDef("test_simplest", "Test Simplest", 90000);
		deleteProc("test_simplest");
		logout();
		log.info("------ END runProcessTest ------");
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
