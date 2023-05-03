package jpl.cws.test.integration.ui;

import jpl.cws.test.WebTestUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author awilhelm
 *
 */
@Ignore
public class TasklistTestIT extends WebTestUtil {
    private static final Logger log = LoggerFactory.getLogger(TasklistTestIT.class);
    private static int testCasesCompleted = 0;

    @Test
    public void runCreateTaskTest() {
        Boolean scriptPass = false;
        try {
            log.info("------ START TasklistTestIT:runCreateTaskTest ------");

            gotoLoginPage();
            login();
            gotoDeployments();

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

            // Verify task creation.
            log.info("Verifying task creation.");
            WebElement taskCount = findElByXPath("//*[contains(@class,'counter')]");
            if (taskCount.getText().equals("1")) {
                log.info("1 task created.");
            } else {
                log.info("There was either 0 or more than 1 tasks in the task list.");
                log.info("Not all test cases passed. Only " + testCasesCompleted + "/2 passed.");
                assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
            }
            sleep(1000);

            findOnPage("Test Task");
            WebElement createdTask = findElByXPath("//*[contains(text(),'Test Task')]");
            createdTask.click();
            sleep(1000);

            // Ensure that only the assigned user can complete a task.
            log.info("Verifying user access.");
            runUserAccessTest();
            sleep(1000);

            log.info("Completing the task.");
            WebElement completeButton = findElByXPath("//*[contains(text(),'Complete')]");
            completeButton.click();
            sleep(1000);
            scriptPass = true;
            testCasesCompleted++;

            log.info("------ END TasklistTestIT:runCreateTaskTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }

    public void runUserAccessTest(){
        Boolean scriptPass = false;

        try {
            log.info("------ START TasklistTestIT:runUserAccessTest ------");

            // Check that non-assigned users cannot edit the task.
            WebElement completeButton = findElByXPath("//*[contains(text(),'Complete')]");
            if (!completeButton.isEnabled()) {
                scriptPass = true;
                log.info("Verified complete button is not clickable.");
            } else {
                log.info("Complete button was clickable when user was not assigned to task.");
                log.info("Not all test cases passed. Only " + testCasesCompleted + "/2 passed.");
                assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
            }

            // Claim the task.
            log.info("Claiming task.");
            WebElement claimButton = findElByXPath("//*[contains(@class,'claim')]");
            claimButton.click();
            sleep(1000);

            // Check that assigned users can edit the task.
            if (completeButton.isEnabled()) {
                scriptPass = true;
                log.info("Verified complete button is clickable.");
            } else {
                log.info("Complete button was not clickable when user was assigned to task.");
                log.info("Not all test cases passed. Only " + testCasesCompleted + "/2 passed.");
                assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
            }
            testCasesCompleted++;
            log.info("------ END TasklistTestIT:runUserAccessTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        assertTrue("Tasklist Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }
    // Add more tasklist page tests here
}
