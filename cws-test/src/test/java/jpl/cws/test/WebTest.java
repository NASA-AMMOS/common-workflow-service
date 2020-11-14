package jpl.cws.test;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ghollins
 *
 */
public class WebTest extends WebTestUtil {	
	private static final Logger log = LoggerFactory.getLogger(WebTest.class);

	@Test
	@Ignore (value="Not portable (specific to Hollins/local-setup)")
	public void loginTest() {
		log.info("------ START loginTest ------");
		gotoLoginPage();
		login();
		logout();
		log.info("------ END loginTest ------");
	}
	
	@Test
	@Ignore (value="Not portable (specific to Hollins/local-setup)")
	public void deployTest() {
		log.info("------ START deployTest ------");
		gotoLoginPage();
		login();
		uploadProcessDefinitionFile();
		logout();
		log.info("------ END deployTest ------");
	}
	
	@Test
	@Ignore (value="Not portable (specific to Hollins/local-setup)")
	public void runProcessTest() {
		log.info("------ START runProcessTest ------");
		gotoLoginPage();
		login();
		startProcessFromConsole("test_simplest");
		logout();
		log.info("------ END runProcessTest ------");
	}
	
	private void uploadProcessDefinitionFile() {
		WebElement fileUploadInput = findElById("file");
		fileUploadInput.sendKeys(TEST_BPMN_DIR+"/test_simplest.bpmn");
		
		WebElement deployProcDefBtn = findElById("deployProcDefBtn");
		deployProcDefBtn.click();
		findOnPage("Deployed process definition");
		findOnPage("test_simplest");
	}
	
	private void startProcessFromConsole(String procDefKey) {
		findOnPage(procDefKey);
		WebElement startIcon = findElById("start_"+procDefKey+"_icon");
		log.info("Clicking on " + startIcon);
		startIcon.click();
		findOnPage("Scheduled the '"+procDefKey+"' process.");
	}
	
	
}
