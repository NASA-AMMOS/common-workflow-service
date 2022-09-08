package jpl.cws.test.ldap.ui;

import jpl.cws.test.WebTestUtil;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rfray
 *
 */
public class WebTestLdap extends WebTestUtil {
    private static final Logger log = LoggerFactory.getLogger(jpl.cws.test.WebTestIT.class);

    @Test
    public void loginTest() {
        log.info("------ START loginTest ------");
        gotoLoginPage();
        login();
        logout();
        log.info("------ END loginTest ------");
    }

    @Test
    public void userInfoTest() {
        log.info("------ START userInfoTest ------");
        gotoLoginPage();
        login();
        gotoDeployments();

        // Go to Camunda Cockpit
        log.info("Clicking Cockpit button.");
        waitForElementXPath("//a[@href='/camunda/app/cockpit']");
        WebElement cockpit = driver.findElement(By.xpath("//a[@href='/camunda/app/cockpit']"));
        cockpit.click();
        findOnPage("Camunda Cockpit | Dashboard");
        sleep(2000);

        // Find first and last name from LDAP server
        findOnPage("Ronny Fray");
        sleep(2000);

        // Go back to CWS
        WebElement cws = driver.findElement(By.xpath("//a[@href='/cws-ui']"));
        cws.click();
        findOnPage("CWS - Deployments");
        logout();
        log.info("------ END userInfoTest ------");
    }
}
