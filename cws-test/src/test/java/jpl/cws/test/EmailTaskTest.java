package jpl.cws.test;

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
import org.junit.*;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Tests related to EmailTask
 * 
 */
public class EmailTaskTest {
	
	@Rule
	public ProcessEngineRule processEngineRule = new ProcessEngineRule();
	
    private GreenMail greenMail;
	private static final int SMTP_PORT = 2525;

    @Before // deciding where to ultimately put the jUnit integration
    public void setUp() {
        ServerSetup smtpSetup = new ServerSetup(SMTP_PORT, null, ServerSetup.PROTOCOL_SMTP);
        greenMail = new GreenMail(smtpSetup);
        greenMail.start();
    }

	@After
    public void tearDown() {
        Mocks.reset();
        greenMail.stop();
    }


	/**
	 * Tests BPMN process that sends an email using EmailTask.
	 * 
	 */
	@Test
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

        // Verify email arrival using GreenMail
        assertTrue("Expected to receive exactly one email, but got: "+greenMail.getReceivedMessages().length, greenMail.getReceivedMessages().length == 1);
        MimeMessage email = greenMail.getReceivedMessages()[0];
        try {
            assertTrue(email.getSubject().equals("test from CWS"));
            String body = extractBody(email);
            System.out.println("BODY: " + body);
            // verify newlines got translated correctly
            assertTrue(body.contains("this<br/>has<br/>line<br/>breaks!"));
            // verify to/from
            assertTrue(email.getFrom()[0].toString().contains("user@domain.com"));
            assertTrue(email.getAllRecipients()[0].toString().contains("user@domain.org"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception reading email: "+e);
        }

    }

    private String extractBody(MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof MimeMultipart) {
            return getTextFromMultipart((MimeMultipart) content);
        }
        if (content instanceof Multipart) {
            return getTextFromMultipart((Multipart) content);
        }
        return String.valueOf(content);
    }

    private String getTextFromMultipart(Multipart multipart) throws Exception {
        String html = null;
        String plain = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            Part part = multipart.getBodyPart(i);
            Object partContent = part.getContent();
            String contentType = part.getContentType();
            if (partContent instanceof String) {
                String text = (String) partContent;
                if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                    html = text;
                } else if (contentType != null && contentType.toLowerCase().contains("text/plain")) {
                    plain = text;
                }
            } else if (partContent instanceof Multipart) {
                String nested = getTextFromMultipart((Multipart) partContent);
                if (nested != null && !nested.isEmpty()) {
                    // Prefer nested html if available
                    if (part.getContentType().toLowerCase().contains("text/html")) {
                        html = nested;
                    } else if (plain == null) {
                        plain = nested;
                    }
                }
            }
        }
        if (html != null) return html;
        if (plain != null) return plain;
        return "";
	}

}