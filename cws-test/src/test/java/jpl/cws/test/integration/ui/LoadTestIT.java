package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author rfray
 *
 */
public class LoadTestIT extends WebTestUtil {
    private static final Logger log = LoggerFactory.getLogger(LoadTestIT.class);
    private static int testCasesCompleted = 0;

    @Test
    public void runLargeLoadTest() throws IOException {
        Boolean scriptPass = false;
        try {
            log.info("------ START LoadTestIT:runLargeLoadTest ------");
            gotoLoginPage();
            login();

            goToPage("deployments");

            // prepare 5 process definitions to run at the same time
            deployFile("external_pwd");
            enableWorkers("external_pwd");

            deployFile("test_simplest");
            enableWorkers("test_simplest");

            deployFile("test_initiators_page");
            enableWorkers("test_initiators_page");

            deployFile("test_hello_world");
            enableWorkers("test_hello_world");

            deployFile("test_logs_page");
            enableWorkers("test_logs_page");


            // run 1000 models, 5 at a time (across 3 workers ideally)
            runStartLoadTest();

            if (Integer.toString(testCasesCompleted).equals("1")) {
                scriptPass = true;
            } else {
                log.info("Not all test cases passed. Only "+ testCasesCompleted + "/1 passed.");
            }

            log.info("------ END LoadTestIT:runLargeLoadTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        deleteProc("external_pwd");
        deleteProc("test_simplest");
        deleteProc("test_initiators_page");
        deleteProc("test_hello_world");
        deleteProc("test_logs_page");
        logout();
        assertTrue("Load Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
    }

    public void runStartLoadTest() throws IOException {
        Boolean scriptPass = false;
        WebDriverWait wait = new WebDriverWait(driver,30);
        try {
            log.info("------ START LoadTestIT:runStartLoadTest ------");

            goToPage("initiators");

            log.info("Implementing initiators on Ace Editor...");
            //go into the div element in CWS and paste it there.
            WebElement aceEditor = driver.findElement(By.cssSelector("textarea.ace_text-input"));
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("ace.edit('editorDiv').navigateFileEnd();");
            js.executeScript("ace.edit('editorDiv').setValue('');");
            String initiatorXML = String.join(System.getProperty("line.separator"),
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                    "<beans ",
                    "	xmlns=\"http://www.springframework.org/schema/beans\"",
                    "	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
                    "	xsi:schemaLocation=\"",
                    "		http://www.springframework.org/schema/beans",
                    "		http://www.springframework.org/schema/beans/spring-beans.xsd\">",
                    "",
                    "	<bean id=\"repeat_1\" class=\"jpl.cws.process.initiation.RepeatingDelayInitiator\">",
                    "		<property name=\"procDefKey\" value=\"external_pwd\" />",
                    "		<property name=\"delayBetweenProcesses\" value=\"10\" />",
                    "		<property name=\"maxRepeats\" value=\"200\" />",
                    "		<property name=\"procVariables\">",
                    "			<map>",
                    "				<entry key=\"variable1\" value=\"foo\"></entry>",
                    "				<entry key=\"variable2\" value=\"bar\"></entry>",
                    "			</map>",
                    "		</property>",
                    "	</bean>",
                    "",
                    "",
                    "	<bean id=\"repeat_2\" class=\"jpl.cws.process.initiation.RepeatingDelayInitiator\">",
                    "		<property name=\"procDefKey\" value=\"test_simplest\" />",
                    "		<property name=\"delayBetweenProcesses\" value=\"10\" />",
                    "		<property name=\"maxRepeats\" value=\"200\" />",
                    "		<property name=\"procVariables\">",
                    "			<map>",
                    "				<entry key=\"variable1\" value=\"foo\"></entry>",
                    "				<entry key=\"variable2\" value=\"bar\"></entry>",
                    "			</map>",
                    "		</property>",
                    "	</bean>",
                    "",
                    "",
                    "	<bean id=\"repeat_3\" class=\"jpl.cws.process.initiation.RepeatingDelayInitiator\">",
                    "		<property name=\"procDefKey\" value=\"test_initiators_page\" />",
                    "		<property name=\"delayBetweenProcesses\" value=\"10\" />",
                    "		<property name=\"maxRepeats\" value=\"200\" />",
                    "		<property name=\"procVariables\">",
                    "			<map>",
                    "				<entry key=\"variable1\" value=\"foo\"></entry>",
                    "				<entry key=\"variable2\" value=\"bar\"></entry>",
                    "			</map>",
                    "		</property>",
                    "	</bean>",
                    "",
                    "",
                    "	<bean id=\"repeat_4\" class=\"jpl.cws.process.initiation.RepeatingDelayInitiator\">",
                    "		<property name=\"procDefKey\" value=\"test_hello_world\" />",
                    "		<property name=\"delayBetweenProcesses\" value=\"10\" />",
                    "		<property name=\"maxRepeats\" value=\"200\" />",
                    "		<property name=\"procVariables\">",
                    "			<map>",
                    "				<entry key=\"variable1\" value=\"foo\"></entry>",
                    "				<entry key=\"variable2\" value=\"bar\"></entry>",
                    "			</map>",
                    "		</property>",
                    "	</bean>",
                    "",
                    "",
                    "	<bean id=\"repeat_5\" class=\"jpl.cws.process.initiation.RepeatingDelayInitiator\">",
                    "		<property name=\"procDefKey\" value=\"test_logs_page\" />",
                    "		<property name=\"delayBetweenProcesses\" value=\"10\" />",
                    "		<property name=\"maxRepeats\" value=\"200\" />",
                    "		<property name=\"procVariables\">",
                    "			<map>",
                    "				<entry key=\"variable1\" value=\"foo\"></entry>",
                    "				<entry key=\"variable2\" value=\"bar\"></entry>",
                    "			</map>",
                    "		</property>",
                    "	</bean>",
                    "",
                    "</beans>",
                    "",
                    "");

            aceEditor.sendKeys(initiatorXML.replace("	", ""));

            waitForElementID("saveXmlBtn");
            log.info("Saving changes..");
            driver.findElement(By.id("saveXmlBtn")).click();

            waitForElementID("saveConfirmBtn");
            driver.findElement(By.id("saveConfirmBtn")).click();

            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("saveConfirmBtn")));

            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("toggle_repeat_1")));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("toggle_repeat_2")));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("toggle_repeat_3")));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("toggle_repeat_4")));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("toggle_repeat_5")));

            log.info("Enabling all initiators.");
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("activate-all-inits")));
            WebElement enableAll = findElById("activate-all-inits");


            sleep(7000);
            if(!enableAll.isSelected()) {
                js.executeScript("arguments[0].click();", findElById("activate-all-inits"));
                sleep(1000);
            } else {	// toggle off and on to start
                js.executeScript("arguments[0].click();", findElById("activate-all-inits"));
                sleep(1000);
                js.executeScript("arguments[0].click();", findElById("activate-all-inits"));
                sleep(1000);
            }

            procCounter = 1000 + procCounter; //for the 1000 procs started.

            goToPage("deployments");

            log.info("Changing status refresh to 1 second.");
            Select select = new Select(findElById("refresh-rate"));
            select.selectByValue("1");

            int counter = 0;
            while(counter < 7) {
                sleep(90000);
                checkIdle();    // check if the browser has become idle

                WebElement statsText = driver.findElement(By.id("stat-txt-cws-reserved-total"));
                log.info("Getting text from Status Bar of Deployments Page.");
                String child = statsText.getText();
                log.info(child);

                //analyze string to check how many procs completed.
                if (child.contains("completed: 1000")) {
                    log.info("All processes completed.");
                    scriptPass = true;
                    testCasesCompleted++;

                    int totalWaitTime = (counter * 90000) / 1000;
                    log.info("Total duration of load is " + totalWaitTime + " seconds");
                    break;
                }
                counter++;
            }

            log.info("------ END LoadTestIT:runStartLoadTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        screenShot("LoadTestIT-runStartLoadTest");
        assertTrue("Start Load Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
    }
}
