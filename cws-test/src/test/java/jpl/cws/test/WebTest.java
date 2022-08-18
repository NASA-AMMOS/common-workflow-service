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
	public void loginTest() {
		log.info("------ START loginTest ------");
		gotoLoginPage();
		login();
		logout();
		log.info("------ END loginTest ------");
	}
	
	@Test
	public void deployTest() {
		log.info("------ START deployTest ------");
		gotoLoginPage();
		login();
		uploadProcessDefinitionFile();
		logout();
		log.info("------ END deployTest ------");
	}
	
	@Test
	public void runProcessTest() {
		log.info("------ START runProcessTest ------");
		gotoLoginPage();
		login();
		startProcDef("test_simplest", "Test Simplest", 90000);
		deleteProc("test_simplest");
		logout();
		log.info("------ END runProcessTest ------");
	}
	
	private void uploadProcessDefinitionFile() {
		WebElement fileUploadInput = findElById("file-input");
		fileUploadInput.sendKeys(TEST_BPMN_DIR+"/test_simplest.bpmn");
		
		WebElement deployProcDefBtn = findElById("bpmn-form");
		deployProcDefBtn.click();
		findOnPage("Deployed process definition");
		findOnPage("test_simplest");
	}


}
