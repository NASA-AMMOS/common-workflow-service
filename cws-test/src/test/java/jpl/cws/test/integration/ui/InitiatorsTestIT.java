package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author hasan
 *
 */
public class InitiatorsTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(InitiatorsTestIT.class);
	private static int testCasesCompleted = 0;

	@Test
	public void runInitiatorsPageTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START InitiatorsTestIT:runInitiatorsPageTest ------");
			gotoLoginPage();
			login();

			goToPage("deployments");

			deployFile("test_initiators_page");

			enableWorkers("test_initiators_page");


			runStartInitiatorTest();
			//runCronInitiatorTest();
			runVariableProcTest();

			if (Integer.toString(testCasesCompleted).equals("2")) {
				scriptPass = true;
			} else {
				log.info("Not all test cases passed. Only "+ testCasesCompleted + "/3 passed.");
			}

			log.info("------ END InitiatorsTestIT:runInitiatorsPageTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		deleteProc("test_initiators_page");
		logout();
		assertTrue("Initiators Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runStartInitiatorTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		try {
			log.info("------ START InitiatorsTestIT:runStartInitiatorTest ------");

			goToPage("initiators");

			log.info("Implementing initiators on Ace Editor...");
			//go into the div element in CWS and paste it there.
			WebElement aceEditor = driver.findElement(By.cssSelector("textarea.ace_text-input"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("ace.edit('editorDiv').navigateFileEnd();");
			js.executeScript("ace.edit('editorDiv').setValue('');");
			String initiatorXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<beans \n" +
				"	xmlns=\"http://www.springframework.org/schema/beans\"\n" +
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"	xsi:schemaLocation=\"\n" +
				"		http://www.springframework.org/schema/beans\n" +
				"		http://www.springframework.org/schema/beans/spring-beans.xsd\">\n" +
				"\n" +
				"	<bean id=\"repeat_1\" class=\"jpl.cws.process.initiation.RepeatingDelayInitiator\">\n" +
				"		<property name=\"procDefKey\" value=\"test_initiators_page\" />\n" +
				"		<property name=\"delayBetweenProcesses\" value=\"1000\" />\n" +
				"		<property name=\"maxRepeats\" value=\"10\" />\n" +
				"		<property name=\"procVariables\">\n" +
				"			<map>\n" +
				"				<entry key=\"variable1\" value=\"foo\"></entry>\n" +
				"				<entry key=\"variable2\" value=\"bar\"></entry>\n" +
				"			</map>\n" +
				"		</property>\n" +
				"	</bean>\n" +
				"\n" +
				"\n" +
				"	<bean id=\"cron_initiator\" class=\"jpl.cws.process.initiation.cron.CronInitiator\">\n" +
				"		<property name=\"procDefKey\"  value=\"test_initiators_page\" />\n" +
				"		<property name=\"cronExpression\"  value=\" 0 0/1 * 1/1 * ? *\" />\n" +
				"		<property name=\"procVariables\">\n" +
				"			<map>\n" +
				"				<entry key=\"variable1\"   value=\"foo\"></entry>\n" +
				"				<entry key=\"variable2\"   value=\"bar\"></entry>\n" +
				"			</map>\n" +
				"		</property>\n" +
				"	</bean>\n" +
				"\n" +
				"</beans>\n" +
				"\n" +
				"\n" +
				"";


			initiatorXML = initiatorXML.replace("\n", "\\n").replace("\"", "\\\"");

			js.executeScript("ace.edit('editorDiv').setValue(\"" + initiatorXML + "\");");

			waitForElementID("saveXmlBtn");
			log.info("Saving changes..");
			driver.findElement(By.id("saveXmlBtn")).click();

			waitForElementID("saveConfirmBtn");
			driver.findElement(By.id("saveConfirmBtn")).click();

			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("saveConfirmBtn")));

			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("toggle_repeat_1")));

			log.info("Enabling repeat initiator.");
			WebElement enableAction = findElById("toggle_repeat_1");

			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("activate-all-inits")));
			WebElement enableAll = findElById("activate-all-inits");

			assert(!enableAll.isSelected()); //verify the enableAll action button is not selected.


			if(!enableAction.isSelected()) {
				js.executeScript("arguments[0].click();", findElById("toggle_repeat_1"));
				sleep(1000);
			} else {	// toggle off and on to start
				js.executeScript("arguments[0].click();", findElById("toggle_repeat_1"));
				sleep(1000);
				js.executeScript("arguments[0].click();", findElById("toggle_repeat_1"));
				sleep(1000);
			}

			procCounter = 10 + procCounter; //for the 10 procs started.

			goToPage("deployments");

			log.info("Changing status refresh to 1 second.");
			Select select = new Select(findElById("refresh-rate"));
			select.selectByValue("1");

			sleep(20000);

			log.info("Getting info from progress bar of Test Initiators Page.");
			WebElement statsText = driver.findElement(By.id("stat-txt-test_initiators_page"));
			String child = statsText.getText();
			log.info(child);

			//analyze string to check how many procs completed.
			if (child.contains("completed: 10")) {
				String color = driver.findElement(By.className("progress-bar-success")).getCssValue("background-color");
				log.info(color);

				if (color.equals("rgba(92, 184, 92, 1)")) { //color = green
					scriptPass = true;
					testCasesCompleted++;
				}
			}

			log.info("------ END InitiatorsTestIT:runStartInitiatorTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("InitiatorTestIT-runStartInitiatorTest");
		assertTrue("Start Initiators test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runVariableProcTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START InitiatorsTestIT:runVariableProcTest ------");
			goToPage("processes");

			log.info("Filtering results for Test Initiators Page test.");
			waitForElementXPath("//div[@id=\'processes-table_filter\']/label/input");

			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).click();
			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).sendKeys("test_initiators_page");
			driver.findElement(By.xpath("//div[@id=\'processes-table_filter\']/label/input")).sendKeys(Keys.ENTER);

			waitForElementID("processes-table");

			log.info("Clicking on Test Initiators Page history.");
			WebElement historyButton = findElByXPath("//a[contains(text(),'History')]");
			waitForElement(historyButton);
			historyButton.sendKeys(Keys.RETURN);

			findOnPage("CWS - History");

			log.info("Looking for 'variable1 = foo' and 'variable2 = bar'");
			if (findOnPage("Setting (string) variable1 = foo")
					&& findOnPage("Setting (string) variable2 = bar")) {
				scriptPass = true;
				testCasesCompleted++;
			}
			log.info("------ END InitiatorsTestIT:runVariableProcTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("InitiatorTestIT-runVariableProcTest");
		assertTrue("Start Initiators test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runCronInitiatorTest() throws IOException {
		Boolean scriptPass = false;
		WebDriverWait wait = new WebDriverWait(driver,Duration.ofSeconds(30));
		try {
			log.info("------ START InitiatorsTestIT:runCronInitiatorTest ------");

			goToPage("workers");
			sleep(3000); //wait for element to be attached to the page.
			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//input[contains(@id,'test_initiators_page_limit')]")));
			WebElement threadLimit = driver.findElement(By.xpath("//input[contains(@id,'test_initiators_page_limit')]"));

			threadLimit.clear();
			threadLimit.sendKeys("10");

			driver.findElement(By.id("workers-table")).click();

			goToPage("initiators");

			WebElement aceEditor = driver.findElement(By.cssSelector("textarea.ace_text-input"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("ace.edit('editorDiv').navigateFileEnd();");
			js.executeScript("ace.edit('editorDiv').setValue('');");
			String initiatorXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<beans \n" +
				"	xmlns=\"http://www.springframework.org/schema/beans\"\n" +
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"	xsi:schemaLocation=\"\n" +
				"		http://www.springframework.org/schema/beans\n" +
				"		http://www.springframework.org/schema/beans/spring-beans.xsd\">\n" +
				"\n" +
				"	<bean id=\"repeat_1\" class=\"jpl.cws.process.initiation.RepeatingDelayInitiator\">\n" +
				"		<property name=\"procDefKey\" value=\"test_initiators_page\" />\n" +
				"		<property name=\"delayBetweenProcesses\" value=\"1000\" />\n" +
				"		<property name=\"maxRepeats\" value=\"10\" />\n" +
				"		<property name=\"procVariables\">\n" +
				"			<map>\n" +
				"				<entry key=\"variable1\" value=\"foo\"></entry>\n" +
				"				<entry key=\"variable2\" value=\"bar\"></entry>\n" +
				"			</map>\n" +
				"		</property>\n" +
				"	</bean>\n" +
				"\n" +
				"\n" +
				"	<bean id=\"cron_initiator\" class=\"jpl.cws.process.initiation.cron.CronInitiator\">\n" +
				"		<property name=\"procDefKey\"  value=\"test_initiators_page\" />\n" +
				"		<property name=\"cronExpression\"  value=\" 0 0/1 * 1/1 * ? *\" />\n" +
				"		<property name=\"procVariables\">\n" +
				"			<map>\n" +
				"				<entry key=\"variable1\"   value=\"foo\"></entry>\n" +
				"				<entry key=\"variable2\"   value=\"bar\"></entry>\n" +
				"			</map>\n" +
				"		</property>\n" +
				"	</bean>\n" +
				"\n" +
				"</beans>\n" +
				"\n" +
				"";


			initiatorXML = initiatorXML.replace("\n", "\\n").replace("\"", "\\\"");

			js.executeScript("ace.edit('editorDiv').setValue(\"" + initiatorXML + "\");");

			waitForElementID("saveXmlBtn");
			driver.findElement(By.id("saveXmlBtn")).click();

			waitForElementID("saveConfirmBtn");
			driver.findElement(By.id("saveConfirmBtn")).click();

			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("saveConfirmBtn")));

			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("toggle_cron_initiator")));

			js.executeScript("arguments[0].click();", findElById("toggle_cron_initiator"));

			sleep(120000); //2 proc defs will run in 120 seconds.

			//stop the cron job so it doesn't continue.
			js.executeScript("arguments[0].click();", findElById("toggle_cron_initiator"));

			goToPage("deployments");

			WebElement statsText = driver.findElement(By.id("stat-txt-test_initiators_page"));
			String child = statsText.getText();
			log.info(child);

			//analyze string to check how many procs completed.
			if (child.contains("completed: 2")) {
				String color = driver.findElement(By.className("progress-bar-success")).getCssValue("background-color");
				log.info(color);

				if (color.equals("rgba(92, 184, 92, 1)")) { //color = green
					scriptPass = true;
					procCounter = procCounter + 2;
					testCasesCompleted++;
				}
			}

			log.info("------ END InitiatorsTestIT:runCronInitiatorTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("InitiatorTestIT-runCronInitiatorTest");
		assertTrue("Cron Initiator test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	// Add more deployment page tests here
}
