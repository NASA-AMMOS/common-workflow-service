package jpl.cws.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests related to Security
 * 
 */
public class InstallerTest {
	private static Charset charset = StandardCharsets.UTF_8;
	private static final String NL  = System.getProperty("line.separator");
	
	@Before
	public void setup() throws IOException {
		shutdownConsole();
	}
	
	@After
	public void tearDown() throws IOException {
		startConsole();
	}
	
	/**
	 * Basic test that simply executes the code.
	 * 
	 * TODO: verify values and other things...
	 */

	@Test
	public void testInstaller() {
		
		Boolean scriptPass = false;
		
		try {
			String s = null;
			String installerPath = System.getProperty("user.dir") + "/src/test/resources/installerTest.sh";
			
			System.out.println(installerPath);
			
			System.out.println("RUNNING installerTest.sh ...");
			Process process = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c", installerPath});
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			// read the output from the command
			System.out.println("\n--------------------------");
			System.out.println("STDOUT OF installerTest.sh:\n");
			
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}

			// read any errors from the attempted command
			System.out.println("\n-----------------------------------");
			System.out.println("STDERR (if any) OF installerTest.sh:\n");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
			
			process.waitFor();
			
			System.out.println("installerTest.sh result code: " + process.exitValue());
			
			if (process.exitValue() == 0) {
				scriptPass = true;
			}
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			
			scriptPass = false;
		}
		
		// Verify some variable values
		assertTrue("Installer Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);

	}
	
	public static void copy(Path from, Path to) throws Exception {
		try {
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Copied " + from + " to " + to);
		} catch (IOException e) {
			throw new Exception("ERROR: Problem copying " + from + " to " + to +" (" + e.toString() + ")");
		}
	}
	
	public static String getFileContents(Path filePath) throws IOException {
		return new String(Files.readAllBytes(filePath), charset).replace("\n", NL);
	}
	
	public static void writeToFile(Path filePath, String content) throws IOException {
		Files.write(filePath, content.getBytes(charset));
	}

	public static void shutdownConsole() throws IOException {
		try {
			String cwsDir = new File(System.getProperty("user.dir")).getParent();
			System.out.println(cwsDir);
			String stopConsoleScript = cwsDir + "/dist/console-only/cws/stop_cws.sh";
			String[] command = {"bash", "-c", "bash " + stopConsoleScript};

			Process proc = Runtime.getRuntime().exec(command);
			proc.waitFor();
			StringBuffer output = new StringBuffer();
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = "";
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}
			System.out.println("### " + output);

			Thread.sleep(150000); // wait for shutdown of console.
		}
		catch (Throwable e) {
			System.out.println(e.toString());
		}
	}

	public static void startConsole() throws IOException {
		try {
			String startConsoleScript = new File(System.getProperty("user.dir")).getParent() + "/dist/console-only/cws/start_cws.sh";
			String commandStart = "bash " + startConsoleScript;

			Process procStart = Runtime.getRuntime().exec(commandStart);
			procStart.waitFor();
			StringBuffer outputStart = new StringBuffer();
			BufferedReader readerStart = new BufferedReader(new InputStreamReader(procStart.getInputStream()));
			String lineStart = "";
			while ((lineStart = readerStart.readLine())!= null) {
				outputStart.append(lineStart + "\n");
			}
			System.out.println("### " + outputStart);

			Thread.sleep(150000);	// wait for start up of console.
		}
		catch (Throwable e) {
			System.out.println(e.toString());
		}
	}

}
