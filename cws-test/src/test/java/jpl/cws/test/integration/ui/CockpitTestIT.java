package jpl.cws.test.integration.ui;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author zmixson
 *
 */
public class CockpitTestIT extends WebTestUtil {
    private static final Logger log = LoggerFactory.getLogger(CockpitTestIT.class);

    @Test
    public void dashboardTest() throws IOException {
        log.info("------ START CockpitTestIT:dashboardTest ------");
        gotoLoginPage();
        login();

        goToPage("deployments");
        sleep(1000);

        startProcDef("external_pwd", "external_pwd", 45000);

        // Go to Cockpit Dashboard page
        WebElement cockpit = findElByXPath("//a[contains(text(),'Cockpit')]");
        cockpit.click();
        sleep(1000);

        // Check if process definition is shown on the dashboard
        String processDefinition = findElByXPath("/html/body/div[2]/div/div[2]/div/div/div/section[2]/div/div/div[1]/a").getText().trim();
        if (processDefinition.equals("0")) {
            log.error("Process definition total is INCORRECT");
            screenShot("CockpitTestIT-dashboardTest");
            Assert.fail("Process definition total is INCORRECT");
        } else {
            log.info("Process definition found");
        }

        deleteProc("external_pwd");

        log.info("------ END CockpitTestIT:dashboardTest ------");
        goToPage("deployments");
        logout();
    }

    @Test
    public void processTest() throws IOException {
        log.info("------ START CockpitTestIT:processTest ------");
        gotoLoginPage();
        login();

        goToPage("deployments");

        deployFile("test_hello_world");

        // Go to Process page
        log.info("Verifying the process is active.");
        WebElement cockpit = findElByXPath("//a[contains(text(),'Cockpit')]");
        cockpit.click();
        sleep(1000);

        WebElement processesPage = findElByXPath("(//a[contains(@href, '#/processes')])[2]");
        processesPage.click();
        sleep(1000);

        WebElement processDefinition = findElByXPath("//a[contains(text(),'Test Hello World')]");
        processDefinition.click();
        sleep(1000);

        WebElement jobDefinitionsPage = findElByXPath("//a[contains(text(),'Job Definitions')]");
        jobDefinitionsPage.click();
        sleep(1000);
        
        findOnPage("Active");
        screenShot("CockpitTestIT-processTest");

        deleteProc("test_hello_world");

        log.info("------ END CockpitTestIT:processTest ------");
        goToPage("deployments");
        logout();
    }

    @Test
    public void humanTasksTest() throws IOException {
        log.info("------ START CockpitTestIT:humanTasksTest ------");
        gotoLoginPage();
        login();

        // Go to Tasklist page
        log.info("Navigating to Tasklist button.");
        WebElement tasklist = findElByXPath("//a[@href='/camunda/app/tasklist']");
        tasklist.click();
        findOnPage("Camunda Tasklist");
        sleep(5000);

        // Create a task
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

        // Ensure a simple filter is applied so created tasks are displayed
        log.info("Checking for simple filter.");
        WebElement simpleFilter = findElByXPath("//*[contains(@class,'filter-hint')]");
        if (simpleFilter.isDisplayed()) {
            log.info("there was no simple filter added");
            simpleFilter.click();
        }
        sleep(1000);

        goToPage("deployments");

        // Go to Human Tasks page
        WebElement cockpit = findElByXPath("//a[contains(text(),'Cockpit')]");
        cockpit.click();
        sleep(1000);

        WebElement humanTasks = findElByXPath("//a[contains(text(),'Human Tasks')]");
        humanTasks.click();
        sleep(1000);

        // Check if human task value is updated
        String humanTasksTotal = findElByXPath("//table[@id='open-task-statistics']/tfoot/tr/th/span").getText().trim();
        if (humanTasksTotal.equals("0")) {
             log.error("Human Task total is INCORRECT");
                screenShot("CockpitTestIT-humanTasksTest");
                Assert.fail("Human Task total is INCORRECT");
        } else {
             log.info("Human Task total is CORRECT");
        }

        goToPage("deployments");

        // Go to Tasklist page
        log.info("Navigating to Tasklist button.");
        tasklist = findElByXPath("//a[@href='/camunda/app/tasklist']");
        tasklist.click();

        findOnPage("Test Task");
        WebElement createdTask = findElByXPath("//*[contains(text(),'Test Task')]");
        createdTask.click();
        sleep(1000);

        // Claim the task
        log.info("Claiming task.");
        WebElement claimButton = findElByXPath("//*[contains(@class,'claim')]");
        claimButton.click();
        sleep(1000);

        WebElement completeButton = findElByXPath("//*[contains(text(),'Complete')]");
        completeButton.click();

        log.info("------ END CockpitTestIT:humanTasksTest ------");
        goToPage("deployments");
        logout();
    }

    @Test
    public void deploymentsTest() throws IOException {
        log.info("------ START CockpitTestIT:deploymentsTest ------");
        gotoLoginPage();
        login();

        goToPage("deployments");

        startProcDef("external_pwd", "external_pwd", 45000);

        // Go to Cockpit Deployments page
        WebElement cockpit = findElByXPath("//a[contains(text(),'Cockpit')]");
        cockpit.click();
        sleep(1000);

        WebElement moreButton = findElByXPath("//a[contains(text(),'More')]");
        moreButton.click();
        sleep(1000);

        WebElement cockpitDeploymentPage = findElByXPath("//a[contains(text(),'Deployments')]");
        cockpitDeploymentPage.click();
        sleep(1000);

        // Check that deployed process is shown
        findOnPage("external_pwd.bpmn");
        screenShot("CockpitTestIT-deploymentsTest");
        sleep(1000);

        deleteProc("external_pwd");

        log.info("------ END CockpitTestIT:deploymentsTest ------");
        goToPage("deployments");
        logout();
    }
    // Add more cockpit page tests here
}
