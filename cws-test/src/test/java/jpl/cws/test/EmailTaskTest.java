package jpl.cws.test;

//import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

/**
 * Tests related to EmailTask
 * 
 */
public class EmailTaskTest {
	
	@Rule
	public ProcessEngineRule processEngineRule = new ProcessEngineRule();
	
	private SimpleSmtpServer smtpServer;
	private static final int SMTP_PORT = 2525;

	@Before // deciding where to ultimately put the jUnit integration
	public void setUp() {
		//MockitoAnnotations.initMocks(this);
		smtpServer = SimpleSmtpServer.start(SMTP_PORT); // need to start on non-privileged port due to permission issues
	}

	@After
	public void tearDown() {
		Mocks.reset();
		smtpServer.stop();
	}


	/**
	 * Tests BPMN process that sends an email using EmailTask.
	 * 
	 */
	//@Test
	@Ignore
	@Deployment(resources = {"bpmn/test_email_task.bpmn"})
	public void testCase1() {
		try {
			ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
					"test_email_task",
					withVariables(
						"smtpPort", SMTP_PORT+"",
						"body", "this\nhas\nline\nbreaks!")
					);
			
			assertThat(processInstance).isEnded();
		} catch(Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}

		// Leverage Dumbster mock SMTP server to check email arrival
		//
		assertTrue("Expected to receive exactly one email, but got: "+smtpServer.getReceivedEmailSize(), smtpServer.getReceivedEmailSize() == 1);
		Iterator<SmtpMessage> emailIter = smtpServer.getReceivedEmail();
		SmtpMessage email = emailIter.next();
		assertTrue(email.getHeaderValue("Subject").equals("test from CWS"));
		
		System.out.println("BODY: " + email.getBody());
		
		// verify newlines got translated correctly
		assertTrue(email.getBody().contains("this<br/>has<br/>line<br/>breaks!"));
		
		// verify to/from
		assertTrue(email.getHeaderValue("From").equals("user@localhost"));
		assertTrue(email.getHeaderValue("To").equals("user@localhost"));
	}

}