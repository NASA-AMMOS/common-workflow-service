package jpl.cws.test.integration.ui;

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
    public void processesTest() {
        log.info("------ START CockpitTestIT:processesTest ------");
        gotoLoginPage();
        login();

        goToPage("deployments");

        deployFile("test_hello_world");

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

        WebElement jobDefinitions = findElByXPath("//a[contains(text(),'Job Definitions')]");
        jobDefinitions.click();
        sleep(1000);
        findOnPage("Active");

        deleteProc("test_hello_world");

        log.info("------ END CockpitTestIT:processesTest ------");
        goToPage("deployments");
        logout();
    }
}
