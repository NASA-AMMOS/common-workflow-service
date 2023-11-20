package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author alopez
 *
 */
public class AdminTestIT extends WebTestUtil {
    private static final Logger log = LoggerFactory.getLogger(AdminTestIT.class);

    @Test
    public void runUsersTest() {
        Boolean scriptPass = false;
        try {
            log.info("------ START AdminTestIT:runUsersTest ------");
            gotoLoginPage();
            login();

            goToPage("deployments");

            log.info("Navigating to Admin page...");
            WebElement admin = findElByXPath("//*[contains(text(),'Admin')]");
            admin.click();
            sleep(5000);

            // Go to Users page
            WebElement dashboard = findElByXPath("//*[contains(text(),'Dashboard')]");
            dashboard.click();
            sleep(1000);

            log.info("Checking IDs in List of Users for " + USERNAME + ".");
            WebElement users = findElByXPath("//*[contains(@class, 'section-title')]//*[text() = 'Users']");
            users.click();
            sleep(1000);

            if (findOnPage(USERNAME)) {
                scriptPass = true;
            }
            sleep(1000);
            screenShot("AdminTestIT-runUsersTest");

            goToPage("deployments");
            logout();
            log.info("------ END AdminTestIT:runUsersTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        assertTrue("Admin Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }

    @Test
    public void runGroupsTest() {
        Boolean scriptPass = false;
        try {
            log.info("------ START AdminTestIT:runGroupsTest ------");
            gotoLoginPage();
            login();

            goToPage("deployments");

            log.info("Navigating to Admin page...");
            WebElement admin = findElByXPath("//*[contains(text(),'Admin')]");
            admin.click();
            sleep(5000);

            // Go to Group Authorizations page
            WebElement groupAuthorizationsPage = findElByXPath("(//a[contains(text(),'Group')])[2]");
            groupAuthorizationsPage.click();
            sleep(1000);

            // Create new group authorization
            log.info("Creating group authorization for " + USERNAME + ".");
            WebElement createAuthorizationButton = findElByXPath("//a[contains(text(),'Create new authorization')]");
            createAuthorizationButton.click();
            sleep(1000);

            WebElement groupOrUserToggle = driver.findElement(By.cssSelector(".input-group-addon"));
            groupOrUserToggle.click();
            WebElement userIdField = findElByXPath("//input[@type='text']");
            userIdField.sendKeys(USERNAME);
            sleep(1000);

            WebElement submitButton = findElByXPath("//button[@type='submit']");
            submitButton.click();
            sleep(1000);

            // Go to Groups page
            log.info("Navigating to Groups page...");
            WebElement dashboard = findElByXPath("//a[contains(text(),'Dashboard')]");
            dashboard.click();
            sleep(1000);

            WebElement groupsPage = findElByXPath("(//a[contains(text(),'Groups')])[2]");
            groupsPage.click();
            sleep(1000);

            // Create new group
            WebElement createGroupButton = findElByXPath("//a[contains(text(),'Create new group')]");
            createGroupButton.click();
            sleep(1000);

            log.info("Creating new group " + USERNAME + ".");
            WebElement groupIdField = findElByXPath("//input[@id='inputGroupId']");
            groupIdField.sendKeys(USERNAME);
            sleep(1000);

            WebElement groupNameField = findElByXPath("//input[@id='inputName']");
            groupNameField.sendKeys("cwstestdev");
            sleep(1000);

            WebElement groupTypeField = findElByXPath("//input[@id='inputType']");
            groupTypeField.sendKeys("SYSTEM");
            sleep(1000);

            WebElement confirmGroupButton = findElByXPath("//button[contains(.,'Create new group')]");
            confirmGroupButton.click();
            sleep(1000);
            findOnPage("Successfully created new group " + USERNAME);

            // Delete new group
            log.info("Deleting group " + USERNAME + ".");
            WebElement editButton = findElByXPath("//a[contains(@href,'#/groups/" + USERNAME + "')]");
            editButton.click();
            sleep(1000);

            WebElement deleteGroupButton = findElByXPath("//button[contains(.,'Delete Group')]");
            deleteGroupButton.click();
            sleep(1000);

            WebElement proceedButton = findElByXPath("//button[contains(.,'Proceed')]");
            proceedButton.click();
            sleep(1000);
            findOnPage("Group " + USERNAME + " successfully deleted");

            // Delete new authorization
            log.info("Deleting group authorization for " + USERNAME + ".");
            WebElement authorizationsPage = findElByXPath("//a[contains(text(),'Authorizations')]");
            authorizationsPage.click();
            sleep(1000);

            // Re-initialize groupAuthorizationsPage WebElement to handle Stale Element Reference Exception
            groupAuthorizationsPage = findElByXPath("(//a[contains(text(),'Group')])[2]");
            groupAuthorizationsPage.click();
            sleep(1000);

            WebElement deleteButton = findElByXPath("(//a[contains(text(),'Delete')])[2]");
            deleteButton.click();
            sleep(1000);

            WebElement confirmDeleteButton = findElByXPath("//button[contains(.,'Delete')]");
            confirmDeleteButton.click();
            sleep(1000);
            findOnPage("The authorization has been deleted successfully.");

            WebElement okButton = findElByXPath("//button[contains(.,'OK')]");

            if (okButton.isDisplayed()) {
                okButton.click();
                scriptPass = true;
            }
            sleep(1000);
            screenShot("AdminTestIT-runGroupsTest");

            goToPage("deployments");
            logout();
            log.info("------ END AdminTestIT:runGroupsTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        assertTrue("Admin Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }

    @Test
    public void runTenantsTest() {
        Boolean scriptPass = false;
        try {
            log.info("------ START AdminTestIT:runTenantsTest ------");
            gotoLoginPage();
            login();

            goToPage("deployments");

            // Go to Tenants page
            log.info("Navigating to Admin page...");
            WebElement admin = findElByXPath("//*[contains(text(),'Admin')]");
            admin.click();
            sleep(5000);

            WebElement dashboard = findElByXPath("//*[contains(text(),'Dashboard')]");
            dashboard.click();
            sleep(1000);

            log.info("Navigating to Tenants page...");
            WebElement tenantsPage = findElByXPath("//*[contains(text(),'Tenants')]");
            tenantsPage.click();
            sleep(1000);

            WebElement newTenantButton = findElByXPath("//a[contains(text(),'Create new tenant')]");
            newTenantButton.click();
            sleep(1000);

            // Create new tenant
            log.info("Creating tenant cwstestdev");
            WebElement tenantIdField = findElById("inputTenantId");
            tenantIdField.sendKeys("cwstestdev");
            sleep(1000);

            WebElement tenantNameField = findElById("inputTenantName");
            tenantNameField.sendKeys(USERNAME);
            sleep(1000);

            WebElement createTenantButton = findElByXPath("//button[contains(.,'Create new tenant')]");
            createTenantButton.click();
            sleep(1000);

            findOnPage("Created new tenant cwstestdev");

            // Delete new tenant
            log.info("Deleting tenant cwstestdev");
            WebElement editButton = findElByXPath("//a[contains(text(),'Edit')]");
            editButton.click();
            sleep(1000);

            WebElement deleteTenantButton = findElByXPath("//button[contains(.,'Delete Tenant')]");
            deleteTenantButton.click();
            sleep(1000);

            WebElement proceedButton = findElByXPath("//button[contains(.,'Proceed')]");
            proceedButton.click();
            sleep(1000);

            if (findOnPage("Tenant cwstestdev successfully deleted.")) {
                scriptPass = true;
            }
            sleep(1000);
            screenShot("AdminTestIT-runTenantsTest");

            goToPage("deployments");
            logout();
            log.info("------ END AdminTestIT:runTenantsTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        assertTrue("Admin Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }

    @Test
    public void runAuthorizationsTest() {
        Boolean scriptPass = false;
        try {
            log.info("------ START AdminTestIT:runAuthorizationsTest ------");
            gotoLoginPage();
            login();

            goToPage("deployments");

            // Go to Authorizations page
            log.info("Navigating to Admin page...");
            WebElement admin = findElByXPath("//*[contains(text(),'Admin')]");
            admin.click();
            sleep(5000);

            WebElement dashboard = findElByXPath("//*[contains(text(),'Dashboard')]");
            dashboard.click();
            sleep(1000);

            log.info("Navigating to Authorizations page...");
            WebElement authorizationsPage = findElByXPath("//*[contains(text(),'Authorizations')]");
            authorizationsPage.click();
            sleep(1000);

            // Create new application authorization
            log.info("Creating application authorization for cwstestdev");
            WebElement addAuthorizationButton = findElByXPath("//*[contains(text(),'Create new authorization')]");
            addAuthorizationButton.click();
            sleep(1000);

            WebElement userIdField = findElByXPath("//input[@type='text']");
            userIdField.sendKeys("cwstestdev");
            sleep(1000);

            WebElement resourceIdField = findElById("inputResourceId");
            resourceIdField.clear();
            resourceIdField.sendKeys("cockpit");
            sleep(1000);

            WebElement submitButton = findElByXPath("//button[@type='submit']");
            submitButton.click();
            sleep(1000);

            findOnPage("cwstestdev");

            // Delete application authorization
            log.info("Deleting application authorization for cwstestdev");
            WebElement deleteButton = findElByXPath("(//a[contains(text(),'Delete')])[2]");
            deleteButton.click();
            sleep(1000);

            WebElement confirmDeleteButton = findElByXPath("//button[contains(.,'Delete')]");
            confirmDeleteButton.click();
            sleep(1000);

            findOnPage("The authorization has been deleted successfully.");

            WebElement okButton = findElByXPath("//button[contains(.,'OK')]");

            if (okButton.isDisplayed()) {
                okButton.click();
                scriptPass = true;
            }
            sleep(1000);
            screenShot("AdminTestIT-runAuthorizationsTest");

            goToPage("deployments");
            logout();
            log.info("------ END AdminTestIT:runAuthorizationsTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        assertTrue("Admin Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }

    @Test
    public void runSystemsTest() {
        Boolean scriptPass = false;
        try {
            log.info("------ START AdminTestIT:runSystemsTest ------");
            gotoLoginPage();
            login();

            goToPage("deployments");

            // Go to Systems page
            log.info("Navigating to Admin page..");
            WebElement admin = findElByXPath("//*[contains(text(),'Admin')]");
            admin.click();
            sleep(5000);

            WebElement dashboard = findElByXPath("//*[contains(text(),'Dashboard')]");
            dashboard.click();
            sleep(1000);

            log.info("Navigating to System page...");
            WebElement systemPage = findElByXPath("//*[contains(text(),'System')]");
            systemPage.click();
            sleep(1000);

            // Check process engine status
            log.info("Checking if process engine is up and running");

            if (findOnPage(" is up and running.")) {
                scriptPass = true;
            }
            sleep(1000);
            screenShot("AdminTestIT-runSystemsTest");

            goToPage("deployments");
            logout();
            log.info("------ END AdminTestIT:runSystemsTest ------");
        }
        catch (Throwable e) {
            System.out.println(e.toString());
            scriptPass = false;
        }
        assertTrue("Admin Page Test reported unexpected success value (scriptPass=" + scriptPass + ")", scriptPass);
    }
    // Add more admin page tests here
}
