package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author awilhelm
 *
 */
public class TasklistTestIT extends WebTestUtil {
    private static final Logger log = LoggerFactory.getLogger(TasklistTestIT.class);
    private static int testCasesCompleted = 0;

    @Test
    public void runTasklistPageTest() {
        Boolean scriptPass = false;
        try {
            log.info("------ START DeploymentsTestIT:runTasklistPageTest ------");
            gotoLoginPage();
            login();
            goToPage("deployments");

            runCreateTaskTest();
            runUserRestrictionTest();
            runUserAccessTest();
            runCompleteTaskTest();

            // Verify that all test cases passed successfully.
            if (Integer.toString(testCasesCompleted).equals("4")) {
                scriptPass = true;
            } else {
                log.info("Not all test cases passed. Only " + testCasesCompleted + "/4 passed.");
            }

            log.info("------ END DeploymentsTestIT:runTasklistPageTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        goToPage("deployments");
        logout();
        assertTrue("Deployments Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
    }

    public void runCreateTaskTest() throws IOException {
        Boolean scriptPass = false;
        try {
            log.info("------ START TasklistTestIT:runCreateTaskTest ------");

            // Navigate to tasklist page.
            log.info("Navigating to Tasklist button.");
            WebElement tasklist = findElByXPath("//a[@href='/camunda/app/tasklist']");
            tasklist.click();
            findOnPage("Camunda Tasklist");
            sleep(5000);

            // Create a task.
            log.info("Creating a task.");
            WebElement createTask = findElByXPath("//a[contains(.,' Create task')]");
            createTask.click();
            sleep(1000);

            log.info("Entering task name.");
            WebElement taskName = findElByXPath("//*[contains(@name,'taskName')]");
            taskName.sendKeys("Test Task");
            sleep(1000);

            log.info("Entering task tenant ID.");
            WebElement taskTenantId = findElByXPath("//*[contains(@name,'taskTenantId')]");
            taskTenantId.sendKeys("1234");
            sleep(1000);

            log.info("Entering task description.");
            WebElement taskDescription = findElByXPath("//*[contains(@name,'taskDescription')]");
            taskDescription.sendKeys("Testing the Tasklist page");
            sleep(1000);

            WebElement saveButton = findElByXPath("//*[contains(text(),'Save')]");
            saveButton.click();
            driver.navigate().refresh();
            sleep(1000);

            // Ensure a simple filter is applied so created tasks are displayed.
            log.info("Checking for simple filter.");
            WebElement simpleFilter = findElByXPath("//*[contains(@class,'filter-hint')]");
            if (simpleFilter.isDisplayed()) {
                log.info("there was no simple filter added");
                simpleFilter.click();
            }
            sleep(1000);

            findOnPage("Test Task");
            WebElement createdTask = findElByXPath("//*[contains(text(),'Test Task')]");
            createdTask.click();
            sleep(1000);

            // Verify task creation.
            log.info("Verifying task creation.");
            WebElement taskCounter = findElByXPath("//*[contains(@class,'counter')]");
            if (taskCounter.getText().equals("1")) {
                log.info("1 task created.");
                testCasesCompleted++;
                scriptPass = true;
            } else {
                log.info("There was not exactly 1 task in the task list.");
            }

            log.info("------ END TasklistTestIT:runCreateTaskTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        screenShot("TasklistTestIT-runCreateTaskTest");
        assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }

    public void runUserRestrictionTest() throws IOException {
        Boolean scriptPass = false;
        try {
            log.info("------ START TasklistTestIT:runUserRestrictionTest ------");

            log.info("Verifying user access.");

            // Check that non-assigned users cannot edit the task.
            WebElement completeButton = findElByXPath("//*[contains(text(),'Complete')]");
            if (!completeButton.isEnabled()) {
                scriptPass = true;
                log.info("Verified complete button is not clickable.");
                testCasesCompleted++;
            } else {
                log.info("Complete button was clickable when user was not assigned to task.");
            }
            log.info("------ END TasklistTestIT:runUserRestrictionTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        screenShot("TasklistTestIT-runUserRestrictionTest");
        assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }

    public void runUserAccessTest() throws IOException {
        Boolean scriptPass = false;
        try {
            log.info("------ START TasklistTestIT:runUserAccessTest ------");

            // Claim the task.
            log.info("Claiming task.");
            WebElement claimButton = findElByXPath("//*[contains(@class,'claim')]");
            claimButton.click();
            sleep(1000);

            // Check that assigned users can edit the task.
            WebElement completeButton = findElByXPath("//*[contains(text(),'Complete')]");
            if (completeButton.isEnabled()) {
                scriptPass = true;
                log.info("Verified complete button is clickable.");
                testCasesCompleted++;
            } else {
                log.info("Complete button was not clickable when user was assigned to task.");
            }
            log.info("------ END TasklistTestIT:runUserAccessTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        screenShot("TasklistTestIT-runUserAccessTest");
        assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }

    public void runCompleteTaskTest() throws IOException {
        Boolean scriptPass = false;
        try {
            log.info("------ START TasklistTestIT:runCompleteTaskTest ------");

            log.info("Completing the task.");
            WebElement completeButton = findElByXPath("//*[contains(text(),'Complete')]");
            completeButton.click();
            sleep(1000);
            WebElement taskCounter = findElByXPath("//*[contains(@class,'counter')]");
            if (taskCounter.getText().equals("0")) {
                scriptPass = true;
                log.info("Verified 0 tasks in tasklist.");
                testCasesCompleted++;
            } else {
                log.info("There was not exactly 0 tasks in the task list.");
            }
            log.info("------ END TasklistTestIT:runCompleteTaskTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        screenShot("TasklistTestIT-runCompleteTaskTest");
        assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }
    // Add more tasklist page tests here
}
