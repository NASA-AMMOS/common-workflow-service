package jpl.cws.test.integration.ui;

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

        WebElement users = findElByXPath("//*[contains(text(),'Users')]");
        users.click();
        sleep(1000);

        log.info("Checking IDs in List of Users for " + USERNAME);
        findOnPage(USERNAME);

        goToPage("deployments");
        logout();
        log.info("------ END AdminTestIT:runUsersTest ------");
    }
}
