package jpl.cws.test.integration.ui;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author alopez
 *
 */
@Ignore
public class AdminTestIT extends WebTestUtil {
    private static final Logger log = LoggerFactory.getLogger(AdminTestIT.class);

    @Test
    public void runUsersTest() {
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
        findOnPage(USERNAME);

        goToPage("deployments");
        logout();
        log.info("------ END AdminTestIT:runUsersTest ------");
    }

    @Test
    public void runGroupsTest() {
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
        okButton.click();
        sleep(1000);

        goToPage("deployments");
        logout();
        log.info("------ END AdminTestIT:runGroupsTest ------");
    }
}
