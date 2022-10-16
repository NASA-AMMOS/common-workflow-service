package jpl.cws.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 *
 * @author ghollins
 *
 */
public class WebTestUtil {
	private static final Logger log = LoggerFactory.getLogger(WebTestUtil.class);

	private static Charset charset = StandardCharsets.UTF_8;
	private static final String NL  = System.getProperty("line.separator");

	protected final static String USERNAME = "cwsci";
	protected final static String PASSWORD = "changeme";
	protected final static String TEST_BPMN_DIR = System.getProperty("user.dir") + "/src/test/resources/bpmn";
	protected static String HOSTNAME = "";
	protected final static String PORT = "38080";
	public static int procCounter = 0;


	private enum DriverType {
		CHROME,
		PHANTOMJS; // TO POSSIBLY BE IMPLEMENTED IN THE FUTURE TO SUPPORT HEADLESS TESTING
	}

	private final DriverType DRIVER_TYPE = DriverType.CHROME;

	protected static WebDriver driver;
	//protected static String userPass;
	private boolean mAutoQuitDriver = true;


	@BeforeClass
	public static void configure() throws IOException {
		log.info("configure...");

		log.info("TEST_BPMN_DIR: " + TEST_BPMN_DIR);

		Path hostnameFile = Paths.get(System.getProperty("user.dir") + "/src/test/resources/hostname.txt");
		HOSTNAME = getFileContents(hostnameFile).trim();
		System.out.println("Using hostname: " + HOSTNAME);
		System.out.println("Current path: " + System.getenv("PATH"));
	}

	@Before
	public void prepareDriver() throws Exception {
		switch (DRIVER_TYPE) {
			case CHROME:
				initChromeDriver();
				break;

			default:
				log.error("Driver type: " + DRIVER_TYPE + " not supported");
				break;
		}
	}

	protected static String getFileContents(Path filePath) throws IOException {
		return new String(Files.readAllBytes(filePath), charset).replace("\n", NL);
	}

	protected WebDriver getDriver() {
		return driver;
	}

	protected void disableAutoQuitDriver() {
		mAutoQuitDriver = false;
	}

	protected void enableAutoQuitDriver() {
		mAutoQuitDriver = true;
	}

	protected boolean isAutoQuitDriverEnabled() {
		return mAutoQuitDriver;
	}

	@After
	public void quitDriver() {
		if (mAutoQuitDriver && driver != null) {
			driver.quit();
			driver = null;
		}
	}


	protected void initChromeDriver() {

		ChromeOptions chromeOptions = new ChromeOptions();

		// Turn on headless mode for Bamboo
		chromeOptions.setHeadless(true);
		chromeOptions.setAcceptInsecureCerts(true);
		chromeOptions.addArguments("--window-size=1920,1080");
		chromeOptions.addArguments("--no-sandbox");
		// chromeOptions.addArguments("--disable-dev-shm-usage");

		WebDriverManager.chromedriver().setup();
		driver = new ChromeDriver(chromeOptions);

		log.info("Driver initialized: " + driver);

		driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
	}

	protected WebElement findElById(String id) {
		return driver.findElement(By.id(id));
	}

	protected WebElement findElByXPath(String path) {
		return driver.findElement(By.xpath(path));
	}

	protected boolean findOnPage(String text) {
		boolean found = driver.getPageSource().contains(text);
		int tries = 0;
		long sleepTime = 4;
		while (!found && tries++ < 13) {
			sleepTime *= 2;
			log.warn("'" + text + "' not found ("+tries+", "+sleepTime+")");
			sleep(sleepTime);
			found = driver.getPageSource().contains(text);
		}

		if (!found) {
			log.error("text '" + text + "' NOT FOUND on page");
			Assert.fail("text '" + text + "' NOT FOUND on page");
		}
		log.info("text '" + text + "' found on page -- [OK]");
		return found;
	}

	protected void gotoDeployments() {
		log.info("navigating to the deployments page...");

		driver.get("http://"+HOSTNAME+":"+PORT + "/cws-ui/deployments");

		if(findOnPage("Deployed Process Definitions")) {
			log.info("Successfully navigated to Deployments page");
		} else {
			log.info("Could not navigate to Deployments page.");
		}
	}


	protected void gotoLoginPage() {
		log.info("navigating to the login page...");

		driver.get("http://"+HOSTNAME+":"+PORT + "/cws-ui/login");
		driver.manage().window().setSize(new Dimension(1024, 768));
		driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
		// Verify we have made it to the Login page
		findOnPage("Login");
	}


	protected void login() {
		WebElement usernameField = findElById("username");
		WebElement passwordField = findElById("password");

		log.info("Entering username...");
		usernameField.sendKeys(USERNAME);

		waitForElement(passwordField);

		log.info("Entering password...");
		passwordField.sendKeys(PASSWORD);

		waitForElementID("submit");

		log.info("Clicking submit button.");
		WebElement submitBtn = findElById("submit");
		submitBtn.click();

		// waitForElementClass("sub-header");

		// Verify we have moved past the login page to the Dashboard
		findOnPage("CWS - Deployments");
	}

	protected void logout() {
		waitForElementID("logoutLink");
		// WebElement submitBtn = driver.findElement(By.id("logoutLink")); //findElById("logoutLink");
		// submitBtn.click();
		driver.get("http://"+HOSTNAME+":"+PORT + "/cws-ui/logout");

		// Verify we have moved to the login page
		findOnPage("Please log in");
	}


	//Demo for Sarjil
	protected void goToPage(String page) {
		log.info("Navigating to " + page + " page");
		driver.get("http://"+HOSTNAME+":"+PORT + "/cws-ui/" + page);
		driver.manage().window().setSize(new Dimension(1024, 768));
		driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
		postLogging(page, "Navigated to ");

	}

	protected void deployFile(String fileName) {
		log.info("Deploying " + fileName);
		WebElement fileUploadInput = findElById("file-input");

		fileUploadInput.sendKeys(TEST_BPMN_DIR+"/" + fileName + ".bpmn");

		findOnPage("Deployed Process Definitions");
		postLogging(fileName, "Successfully deployed ");
		waitForElementID("pv-"+fileName);
	}

	public void startProcDef(String procDef, String procName, long procTime) {
		deployFile(procDef);
		WebDriverWait wait = new WebDriverWait(driver,30);

		wait.until(ExpectedConditions.elementToBeClickable(By.id("pv-"+procDef)));
		WebElement enable = findElById("pv-"+procDef);
		enable.click();
		sleep(1000);

		WebElement allWorkers = findElById("all-workers");
		WebElement allWorkersDone = findElById("done-workers-btn");
		log.info("Enabling workers.");

		if(allWorkers.isSelected()) {
			allWorkersDone.click();
			sleep(1000);
		} else {
			allWorkers.click();
			sleep(1000);
			allWorkersDone.click();
			sleep(1000);
		}

		sleep(2000);

		log.info("Clicking Tasklist button.");
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='/camunda/app/tasklist']")));
		WebElement tasks = driver.findElement(By.xpath("//a[@href='/camunda/app/tasklist']"));
		tasks.click();
		findOnPage("Camunda Tasklist");

		waitForElementXPath("//*[contains(@class,'start-process-action')]");

		log.info("Clicking Start Process button");
		WebElement start = driver.findElement(By.xpath("//*[contains(@class,'start-process-action')]"));
		start.click();

		waitForElementXPath("//input[@placeholder='Search by process name.']");

		log.info("Searching for " + procName + "...");
		WebElement search = driver.findElement(By.xpath("//input[@placeholder='Search by process name.']"));
		search.click();
		search.sendKeys(procName);

		waitForElementXPath("//*[contains(text(), '" + procName + "')]");

		log.info("Clicking on " + procName);
		WebElement li = driver.findElement(By.xpath("//*[contains(text(), '" + procName + "')]"));
		li.click();

		waitForElementXPath("//button[contains(text(),'Start')]");

		log.info("Starting " + procDef);
		WebElement button = driver.findElement(By.xpath("//button[contains(text(),'Start')]"));
		button.click();

		goToPage("deployments");
		//Wait explicitly for process to finish running.
		sleep(procTime);

		procCounter = procCounter + 1;
		log.info("-----------TOTAL PROCS: "+procCounter);
		postLogging(procDef, "Successfully started ");
	}

	public void enableWorkers(String procDef) {
		WebDriverWait wait = new WebDriverWait(driver,30);

		wait.until(ExpectedConditions.elementToBeClickable(By.id("pv-"+procDef)));
		WebElement enable = findElById("pv-"+procDef);
		enable.click();
		sleep(1000);

		WebElement allWorkers = findElById("all-workers");
		WebElement allWorkersDone = findElById("done-workers-btn");
		log.info("Enabling workers.");

		if(allWorkers.isSelected()) {
			allWorkersDone.click();
			sleep(1000);
		} else {
			allWorkers.click();
			sleep(1000);
			allWorkersDone.click();
			sleep(1000);
		}

		sleep(2000);
	}

	public void disableWorkers(String procDef) {
		WebDriverWait wait = new WebDriverWait(driver,30);

		wait.until(ExpectedConditions.elementToBeClickable(By.id("pv-"+procDef)));
		WebElement enable = findElById("pv-"+procDef);
		enable.click();
		sleep(1000);

		WebElement allWorkers = findElById("all-workers");
		WebElement allWorkersDone = findElById("done-workers-btn");
		log.info("Enabling workers.");

		if(allWorkers.isSelected()) {
			allWorkers.click();
			sleep(1000);
			allWorkersDone.click();
			sleep(1000);
		} else {
			allWorkersDone.click();
			sleep(1000);
		}

		sleep(2000);
	}

	public void modifyFile(String filePath, String oldString, String newString)
    {
        File fileToBeModified = new File(filePath);

        String oldContent = "";

        BufferedReader reader = null;

        FileWriter writer = null;
        try
        {
            reader = new BufferedReader(new FileReader(fileToBeModified));

            //Reading all the lines of input text file into oldContent

            String line = reader.readLine();

            while (line != null)
            {
                oldContent = oldContent + line + System.lineSeparator();

                line = reader.readLine();
                log.info("OLD: "+line);
            }

            //Replacing oldString with newString in the oldContent

            String newContent = oldContent.replaceAll(oldString, newString);

            //Rewriting the input text file with newContent

            writer = new FileWriter(fileToBeModified);

            writer.write(newContent);

            log.info("NEW: "+newContent);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            // Close the resources
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	public static void copyFile(File source, File dest) throws IOException {
		Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	public void waitForElement(WebElement arg0) {
	    WebDriverWait wait = new WebDriverWait(driver,30);
	    wait.until(ExpectedConditions.elementToBeClickable(arg0));
	}

	public void waitForElementID(String item) {
	    WebDriverWait wait = new WebDriverWait(driver,30);
	    wait.until(ExpectedConditions.elementToBeClickable(By.id(item)));
	}

	public void waitForElementXPath(String item) {
	    WebDriverWait wait = new WebDriverWait(driver,30);
	    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(item)));
	}

	public void waitForElementClass(String item) {
	    WebDriverWait wait = new WebDriverWait(driver,30);
	    wait.until(ExpectedConditions.elementToBeClickable(By.className(item)));
	}

	public void screenShot(String fileName) throws IOException {
	    // driver is your WebDriver
	   File screenshot = ((TakesScreenshot) driver)
	                            .getScreenshotAs(OutputType.FILE);

	   String rootCWS = System.getProperty("user.dir");
	   File rootFold = new File(rootCWS);

	   String sourcePath = screenshot.toPath().toString();
	   File source = new File(sourcePath);

	   new File(rootFold.getParent() + "/test-screenshots").mkdir(); //makes screenshot dir in cws root dir.
	   String testScreenshots =  rootFold.getParent() + "/test-screenshots/" + fileName + ".png";
	   File newDest = new File(testScreenshots);

	   copyFile(source, newDest); //stores screenshot into the test-screenshot dir.
	   log.info("Saving screenshot as: " + fileName +  " in path: " + testScreenshots);
	   screenshot.deleteOnExit(); //deletes the screenshot stored in var dir.
	}

	public void scrollTo(WebElement element) {
		int elementPosition = element.getLocation().getY();
		String js = String.format("window.scroll(0, %s)", elementPosition);
		((JavascriptExecutor)driver).executeScript(js);
	}

	public void deleteProc(String procName) {
		WebDriverWait wait = new WebDriverWait(driver,30);
		goToPage("deployments");

		if(driver.getPageSource().contains(procName)) {
			wait.until(ExpectedConditions.elementToBeClickable(By.id("pv-"+procName)));
			WebElement enable = findElById("pv-"+procName);
			enable.click();
			sleep(1000);

			WebElement allWorkers = findElById("all-workers");
			WebElement allWorkersDone = findElById("done-workers-btn");

			if(allWorkers.isSelected()) {
				allWorkers.click();
				sleep(1000);
				allWorkersDone.click();
				sleep(1000);
			} else {
				allWorkersDone.click();
				sleep(1000);
			}

			wait.until(ExpectedConditions.elementToBeClickable(By.id("delete-"+procName)));
			WebElement delButton = driver.findElement(By.id("delete-"+procName));
			delButton.click();

			waitForElementID("delete-proc-def");
			findElById("delete-proc-def").click();

			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("delete-proc-def")));
			sleep(3000);
			log.info("*****Successfully deleted " + procName);
			procCounter = procCounter - 1;
			sleep(5000);
		} else {
			log.info(procName + " has already been deleted.");
		}

	}

	public void postLogging(String textToBeFound, String action) {
		if(findOnPage(textToBeFound)) {
			log.info("SUCCESS: " + action + textToBeFound);
		} else {
			log.info("ERROR: Unable to " + action + textToBeFound);
		}
	}
	//--End Demo
	protected void sleep(long millis) {
		try {
			Thread.sleep(millis);
			log.info("Sleeping " + millis + "ms.");
		} catch (InterruptedException e) {
			log.error("InterruptedException during sleep", e);
		}
	}
}
