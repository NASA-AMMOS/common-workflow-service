package jpl.cws.test.integration.ui;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.test.WebTestUtil;

/**
 *
 * @author hasan
 *
 */
@Ignore
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
				log.info("Not all test cases passed. Only "+ testCasesCompleted + "/4 passed.");
			}

			log.info("------ END SnippetsTestIT::runSnippetsPageTest ------");
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			scriptPass = false;
		}
		deleteProc("test_snippets_page");
		logout();
		assertTrue("Initiators Page Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
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
			driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn")).click();

			goToPage("deployments");

			startProcDef("test_snippets_page", "Test Snippets Page");

			goToPage("processes");

			waitForElementID("pd-select");
			log.info("Filter for Test Snippets Page results.");
			Select select = new Select(findElById("pd-select"));
			select.selectByVisibleText("Test Snippets Page");

			waitForElementID("filter-submit-btn");

			WebElement filterSubmit = findElById("filter-submit-btn");
			filterSubmit.click();

			waitForElementID("processes-table");

			log.info("Clicking on Test Snippets Page history.");
			WebElement historyButton = findElByXPath("//button[contains(text(),'History')]");
			waitForElement(historyButton);
			scrollTo(historyButton);
			historyButton.click();

			findOnPage("<title>CWS - History</title>");

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
		screenShot("SnippetsTestIT::runSnippetsModelTest");
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runValidateButtonTest() {
		Boolean scriptPass = false;
		try {
			log.info("------ START SnippetsTestIT:ValidateButtonTest ------");

			goToPage("snippets");

			waitForElementID("validateAndSaveSnippetsSubmitBtn");
			log.info("Clicking on 'Validate and Save' button.");
			driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn")).click();

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
			WebElement aceEditor = driver.findElement(By.cssSelector("textarea.ace_text-input"));

			log.info("Updating snippets through Ace Editor...");
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("ace.edit('editorDiv').navigateFileEnd();");
			js.executeScript("ace.edit('editorDiv').setValue('');");
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
			log.info("Clicking on 'Validate and Save' button...");
			driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn")).click();

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
		screenShot("SnippetsTestIT::runUpdateSnippetsTest");
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runUpdateErrorTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START SnippetsTestIT:UpdateErrorTest ------");
			driver.navigate().refresh();
			sleep(2000);

			WebElement aceEditor = driver.findElement(By.cssSelector("textarea.ace_text-input"));
			log.info("Updating snippets through Ace Editor.");
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("ace.edit('editorDiv').navigateFileEnd();");
			js.executeScript("ace.edit('editorDiv').setValue('');");
			log.info("Initializing snippets to 'Let's get an error!'");
			aceEditor.sendKeys("Let's get an error!");

			waitForElementID("validateAndSaveSnippetsSubmitBtn");
			log.info("Clicking on 'Validate and Save' button");
			driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn")).click();

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
		screenShot("SnippetsTestIT::runUpdateErrorTest");
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}

	public void runReloadEditorTest() throws IOException {
		Boolean scriptPass = false;
		try {
			log.info("------ START SnippetsTestIT:ReloadEditorTest ------");
			driver.navigate().refresh();

			WebElement aceEditor = driver.findElement(By.cssSelector("textarea.ace_text-input"));
			log.info("Updating snippets through Ace Editor.");

			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("ace.edit('editorDiv').navigateFileEnd();");
			js.executeScript("ace.edit('editorDiv').setValue('');");
			log.info("Initializing snippets to 'Let's get an error!'");

			aceEditor.sendKeys("Let's get an error!");

			waitForElementID("validateAndSaveSnippetsSubmitBtn");
			log.info("Clicking on 'Validate and Save' button");
			driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn")).click();

			log.info("Looking for 'ERROR: invalid code.' on page.");
			if(findOnPage("ERROR: invalid code.")) {
				log.info("SUCCESS: Found on 'ERROR: invalid code.' on page.");

				waitForElementID("revertSnippetsSubmitBtn");
				log.info("Clicking on 'Revert Snippets' button...");
				driver.findElement(By.id("revertSnippetsSubmitBtn")).click();

				waitForElementID("validateAndSaveSnippetsSubmitBtn");
				log.info("Clicking on 'Validate and Save' button");
				driver.findElement(By.id("validateAndSaveSnippetsSubmitBtn")).click();

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
		screenShot("SnippetsTestIT::runReloadEditorTest");
		assertTrue("Snippets Model test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);
	}
	// Add more deployment page tests here
}
