package jpl.cws.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests related to Security
 * 
 */
public class SecurityTest {
	private static Charset charset = StandardCharsets.UTF_8;
	private static final String NL  = System.getProperty("line.separator");
	
	@Before
	public void setup() {
		
	}
	
	@After
	public void tearDown() {
		
	}
	
	/**
	 * Basic test that simply executes the code.
	 * 
	 * TODO: verify values and other things...
	 */

	@Test
	public void testCamunda() {
		
		Boolean scriptPass = false;
		
		try {
			copy(Paths.get("./src/test/resources/creds.txt.template"),
				 Paths.get("./src/test/resources/creds.txt"));
			
			Path hostnameFile = Paths.get("./src/test/resources/hostname.txt");
			String hostname = getFileContents(hostnameFile).trim();
			System.out.println("Using hostname: " + hostname);
			
			// Substitute in hostname
			//
			Path filePath = Paths.get("./src/test/resources/creds.txt");
			String content = getFileContents(filePath);
			content = content.replace("__CWS_CONSOLE_HOST__",  hostname);
			writeToFile(filePath, content);
			
			Process catCreds = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c", "cat ./src/test/resources/creds.txt" });
			String s = null;
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(catCreds.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(catCreds.getErrorStream()));

			// read the output from the command
			System.out.println("\n--------------------------");
			System.out.println("CONTENTS OF creds.txt:\n");
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}
			
			System.out.println("RUNNING securityTest.sh ...");
			Process process = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c", "./src/test/resources/securityTest.sh < ./src/test/resources/creds.txt" });
			
			stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			// read the output from the command
			System.out.println("\n--------------------------");
			System.out.println("STDOUT OF securityTest.sh:\n");
			
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}

			// read any errors from the attempted command
			System.out.println("\n-----------------------------------");
			System.out.println("STDERR (if any) OF securityTest.sh:\n");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
			
			process.waitFor();
			
			
			System.out.println("securityTest.sh result code: " + process.exitValue());
			
			if (process.exitValue() == 0) {
				scriptPass = true;
			}
		}
		catch (Throwable e) {
			System.out.println(e.toString());
			
			scriptPass = false;
		}
		
		// Verify some variable values
		assertTrue("Camunda Security Test reported unexpected success value (scriptPass="+scriptPass+")", scriptPass);

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

}
