package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author hasan
 *
 */
public class SnippetsTestIT extends WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(SnippetsTestIT.class);
	private static int testCasesCompleted = 0;

	@Test
	public void runSnippetsPageTest() throws IOException {
		Boolean scriptPass = false;
	try {
			log.info("------ START SnippetsTestIT::runSnippetsPageTest ------");
			gotoLoginPage();
			login();

			runSnippetsModelTest();
			runValidateButtonTest();
			runUpdateSnippetTest();
			runUpdateErrorTest();
			runReloadEditorTest();

			if(Integer.toString(testCasesCompleted).equals("5")) {
				scriptPass = true;
			} else {
				log.info("Not all test cases passed. Only "+ testCasesCompleted + "/5 passed.");
			}

			log.info("------ END SnippetsTestIT::runSnippetsPageTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		deleteProc("test_snippets_page");
		logout();
		assertTrue("Snippets Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runSnippetsModelTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START SnippetsTestIT:SnippetsModelTest ------");

			goToPage("snippets");

			log.info("Clicking on Ace Editor and implementing new snippet: helloWorld();");
			//go into the div element in CWS and paste it there.
			WebElement aceEditor = driver.findElement(By.cssSelector("textarea.ace_text-input"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("ace.edit('editorDiv').navigateFileEnd();");
			js.executeScript("ace.edit('editorDiv').setValue('');");
			log.info("Clearing values inside Ace Editor.");
			aceEditor.sendKeys("package jpl.cws.core.code;\n" +
					"\n" +
					"import java.util.*;\n" +
					"import java.util.regex.*;\n" +
					"import java.io.*;\n" +
					"\n" +
					"//-----------------------------------------------------------------------------\n" +
					"// This class provides a place to define custom methods.\n" +
					"//  Out of the box, the CwsCodeBase superclass provides access to the CWS\n" +
					"//  installation hostname and port via variables:\n" +
					"//    ${cws.hostname}\n" +
					"//    ${cws.port}\n" +
					"//\n" +
					"//  Also, provided by the superclass are these methods:\n" +
					"//    String getEnv(String envVar)\n" +
					"//\n" +
					"//  Example of calling a snippet from a BPMN model:\n" +
					"//    ${cws.getEnv(\"JAVA_HOME\")}\n" +
					"//\n" +
					"//-----------------------------------------------------------------------------\n" +
					"public class CustomMethods extends CwsCodeBase {\n" +
					"    \n" +
					"	public String helloWorld() {\n" +
					"	    return \"Hello World\";\n" +
					"	}\n" +
					"}\n" +
					"");

			waitForElementID("validateAndSaveSnippetsSubmitBtn");
			log.info("Saving snippet changes..");
			WebElement saveButton = driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn"));
			js.executeScript("arguments[0].scrollIntoViewIfNeeded();", saveButton);
			sleep(1000);
			saveButton.click();
			
			// Wait for page to reload after form submission
			sleep(3000); 
			
			// Wait for the page to be on snippets page again after submission
			waitForElementID("statusMessageDiv");
			
			// Check if save was successful - look for success message or absence of error
			WebElement statusDiv = driver.findElement(By.id("statusMessageDiv"));
			String statusText = statusDiv.getText();
			log.info("Status message after save: " + statusText);
			
			if (statusText.contains("ERROR:")) {
				log.error("Failed to save snippets - error message: " + statusText);
				scriptPass = false;
				return;
			}
			
			// Give time for the success message to be visible before navigating away
			sleep(2000);

			goToPage("deployments");

			startProcDef("test_snippets_page", "Test Snippets Page", 90000);

			goToPage("processes");
			sleep(8000);

			waitForElementXPath("//input[@id=\'dt-search-1\']");

			driver.findElement(By.xpath("//input[@id=\'dt-search-1\']")).click();
			driver.findElement(By.xpath("//input[@id=\'dt-search-1\']")).sendKeys("test_snippets_page");
			driver.findElement(By.xpath("//input[@id=\'dt-search-1\']")).sendKeys(Keys.ENTER);

			waitForElementID("processes-table");
			sleep(5000); // Wait longer for process to complete

			log.info("Clicking on Test Snippets Page history.");
			// Wait for History button that is NOT disabled
			waitForElementXPath("//button[contains(text(),'History') and not(contains(@class, 'disabled'))]");
			sleep(1000); // Additional wait to ensure button is ready
			
			WebElement historyButton = findElByXPath("//button[contains(text(),'History') and not(contains(@class, 'disabled'))]");
			js.executeScript("arguments[0].scrollIntoViewIfNeeded();", historyButton);
			sleep(500); // Brief pause after scroll
			
			// Try regular click, use JavaScript if intercepted
			try {
				historyButton.click();
			} catch (ElementClickInterceptedException e) {
				log.info("History button click intercepted, using JavaScript click");
				js.executeScript("arguments[0].click();", historyButton);
			}

			findOnPage("CWS - History");

			log.info("Looking for 'This is our world: Hello World.");
			if(findOnPage("This is our world: Hello World")) {
				log.info("SUCCESS: Found text in history!");
				scriptPass = true;
				testCasesCompleted++;
			}

			log.info("------ END SnippetsTestIT:SnippetsModelTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("SnippetsTestIT-runSnippetsModelTest");
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runValidateButtonTest() {
		Boolean scriptPass = false;
		try {
			log.info("------ START SnippetsTestIT:ValidateButtonTest ------");

			goToPage("snippets");

			waitForElementID("validateAndSaveSnippetsSubmitBtn");
			log.info("Clicking on 'Validate and Save' button.");
			sleep(1000); // Wait for page to be ready
			
			WebElement validateAndSaveButton = driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
	  		js.executeScript("arguments[0].scrollIntoViewIfNeeded();", validateAndSaveButton);
			sleep(500); // Brief pause after scroll
			
			// Try regular click first, if intercepted use JavaScript click
			try {
				validateAndSaveButton.click();
			} catch (ElementClickInterceptedException e) {
				log.info("Regular click intercepted, using JavaScript click");
				js.executeScript("arguments[0].click();", validateAndSaveButton);
			}
			
			// Wait for page to reload after form submission
			sleep(2000);
			waitForElementID("statusMessageDiv");

			log.info("Verifying 'Saved the snippets' shows up on the page.");
			if(findOnPage("Saved the snippets")) {
				log.info("SUCCESS: Found 'Saved the snippets' on page.");
				scriptPass = true;
				testCasesCompleted++;
			}

			log.info("------ END SnippetsTestIT:ValidateButtonTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runUpdateSnippetTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START SnippetsTestIT:UpdateSnippetTest ------");
			driver.navigate().refresh();
			sleep(4000); // Wait for page to reload and Ace Editor to initialize

			log.info("Updating snippets through Ace Editor...");
			JavascriptExecutor js = (JavascriptExecutor) driver;
			// Use JavaScript to set the value directly
			String codeContent = "package jpl.cws.core.code;\n" +
					"\n" +
					"import java.util.*;\n" +
					"import java.util.regex.*;\n" +
					"import java.io.*;\n" +
					"\n" +
					"//-----------------------------------------------------------------------------\n" +
					"// This class provides a place to define custom methods.\n" +
					"//  Out of the box, the CwsCodeBase superclass provides access to the CWS\n" +
					"//  installation hostname and port via variables:\n" +
					"//    ${cws.hostname}\n" +
					"//    ${cws.port}\n" +
					"//\n" +
					"//  Also, provided by the superclass are these methods:\n" +
					"//    String getEnv(String envVar)\n" +
					"//\n" +
					"//  Example of calling a snippet from a BPMN model:\n" +
					"//    ${cws.getEnv(\"JAVA_HOME\")}\n" +
					"//\n" +
					"//-----------------------------------------------------------------------------\n" +
					"public class CustomMethods extends CwsCodeBase {\n" +
					"    \n" +
					"	public String helloWorld() {\n" +
					"	    return \"Hello World\";\n" +
					"	}\n" +
					"}\n" +
					"";
			js.executeScript("ace.edit('editorDiv').setValue(arguments[0]);", codeContent);

			waitForElementID("validateAndSaveSnippetsSubmitBtn");
			log.info("Clicking on 'Validate and Save' button...");

			WebElement validateAndSaveButton = driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn"));
	  		js.executeScript("arguments[0].scrollIntoViewIfNeeded();", validateAndSaveButton);
	  		sleep(2000);

	  		validateAndSaveButton.click();

			log.info("Verifying 'Saved the snippets' shows up on the page.");
			if(findOnPage("Saved the snippets")) {
				log.info("SUCCESS: Found 'Saved the snippets' on page.");
				scriptPass = true;
				testCasesCompleted++;
			}

			log.info("------ END SnippetsTestIT:UpdateSnippetTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("SnippetsTestIT-runUpdateSnippetsTest");
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runUpdateErrorTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START SnippetsTestIT:UpdateErrorTest ------");
			driver.navigate().refresh();
			sleep(4000); // Wait for page to reload and Ace Editor to initialize
			
			log.info("Updating snippets through Ace Editor.");
			JavascriptExecutor js = (JavascriptExecutor) driver;
			log.info("Initializing snippets to 'Let's get an error!'");
			// Use JavaScript to set the value directly
			js.executeScript("ace.edit('editorDiv').setValue(arguments[0]);", "Let's get an error!");

			waitForElementID("validateAndSaveSnippetsSubmitBtn");
			log.info("Clicking on 'Validate and Save' button");

			WebElement validateAndSaveButton = driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn"));
	  		js.executeScript("arguments[0].scrollIntoViewIfNeeded();", validateAndSaveButton);

	  		validateAndSaveButton.click();

			log.info("Looking for 'ERROR: invalid code.' on page.");
			if(findOnPage("ERROR: invalid code.")) {
				log.info("SUCCESS: Found on 'ERROR: invalid code.' on page.");
				scriptPass = true;
				testCasesCompleted++;
			}

			log.info("------ END SnippetsTestIT:UpdateSnippetTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("SnippetsTestIT-runUpdateErrorTest");
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runReloadEditorTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START SnippetsTestIT:ReloadEditorTest ------");
			driver.navigate().refresh();
			sleep(4000); // Wait for page to reload and Ace Editor to initialize
			
			log.info("Updating snippets through Ace Editor.");
			JavascriptExecutor js = (JavascriptExecutor) driver;
			log.info("Initializing snippets to 'Let's get an error!'");
			// Use JavaScript to set the value directly
			js.executeScript("ace.edit('editorDiv').setValue(arguments[0]);", "Let's get an error!");

			waitForElementID("validateAndSaveSnippetsSubmitBtn");
			log.info("Clicking on 'Validate and Save' button");

			WebElement validateAndSaveButton = driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn"));
	  		js.executeScript("arguments[0].scrollIntoViewIfNeeded();", validateAndSaveButton);

			validateAndSaveButton.click();

			log.info("Looking for 'ERROR: invalid code.' on page.");
			if(findOnPage("ERROR: invalid code.")) {
				log.info("SUCCESS: Found on 'ERROR: invalid code.' on page.");

				waitForElementID("revertSnippetsSubmitBtn");
				sleep(1000); // Wait for page to stabilize
				log.info("Clicking on 'Revert Snippets' button...");
				// Get fresh element reference and use JavaScript click to avoid stale element
				WebElement revertButton = driver.findElement(By.id("revertSnippetsSubmitBtn"));
				js.executeScript("arguments[0].click();", revertButton);
				
				sleep(2000); // Wait for page to reload after revert

				waitForElementID("validateAndSaveSnippetsSubmitBtn");
				// Get fresh element reference after page update
				WebElement freshValidateButton = driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn"));
		  		js.executeScript("arguments[0].scrollIntoViewIfNeeded();", freshValidateButton);
		  		sleep(2000);

				log.info("Clicking on 'Validate and Save' button");
				js.executeScript("arguments[0].click();", freshValidateButton);

				log.info("Verifying 'Saved the snippets' shows up on the page.");
				if(findOnPage("Saved the snippets")) {
					log.info("SUCCESS: Found 'Saved the snippets' on page.");
					scriptPass = true;
					testCasesCompleted++;
				}
			}

			log.info("------ END SnippetsTestIT:ReloadEditorTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		screenShot("SnippetsTestIT-runReloadEditorTest");
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	// Add more deployment page tests here
}
