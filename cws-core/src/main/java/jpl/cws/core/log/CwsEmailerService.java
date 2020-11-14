package jpl.cws.core.log;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

public class CwsEmailerService implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(CwsEmailerService.class);
	
	@Value("${cws.notification.emails}") private String prop_cwsNotificationEmails;
	@Value("${cws.smtp.hostname}") private String prop_cwsSMTPHostname;
	@Value("${cws.smtp.port}") private String prop_cwsSMTPPort;
	@Value("${cws.install.hostname}") private String prop_cwsHostName;
	
	@Override
	public void afterPropertiesSet() throws Exception {
	}
	
	/**
	 * Sends a User task assignment notification email to the specified recipients.
	 * 
	 */
	public void sendEmail(
			String subject,
			String message,
			String ... recipients) {
		try {
			Email email = new HtmlEmail();

			log.info("sending email (" +subject + ") to: " + recipients[0] + "...");
			
			String emailSubject = subject;
			String emailBody = message;
			
			// set the body as an HTML message
			((HtmlEmail) email).setHtmlMsg("<html>"
					+ emailBody.replaceAll("\\n", "<br/>").replaceAll("&#10;", "<br/>") + "</html>");// set the alternative text message
			((HtmlEmail) email).setTextMsg("TEXT ALTERNATIVE: " + emailBody.replaceAll("&#10;", "\n"));
			
			email.setHostName(prop_cwsSMTPHostname);		// TODO: make this configurable as well?
			email.setSmtpPort(Integer.parseInt(prop_cwsSMTPPort));
			email.setFrom("cws_admin@locahost");
			email.setSubject(emailSubject);
			
			for (String recip : recipients) {
				email.addTo(recip.trim());
				log.debug("About to send email to " + recip + "...");
			}

			log.debug(" +-----------------------------------------------");
			log.debug(" | SUBJECT     : " + emailSubject);
			log.debug(" | BODY        : " + emailBody);
			log.debug(" | SMTP_HOST   : " + prop_cwsSMTPHostname);
			log.debug(" | SMTP_PORT   : " + prop_cwsSMTPPort);
			log.debug(" +-----------------------------------------------");
			
			// Send email
			//
			email.send();

			log.debug("  Email sent.");
		} catch (Throwable e) {
			log.error("Problem occurred while sending email", e);
			// FIXME: throw?
		}
	}
	
	public void sendNotificationEmails(
			String subject,
			String message) {
		
		subject += " on host " + prop_cwsHostName;
		
		// Always add CWS Hostname to notification messages
		message = "CWS Hostname: " + prop_cwsHostName + "\n\n" + message;
		
		log.debug("sendEmail: subject: " + subject + " Message: " + message + " prop_cwsNotificationEmails: " + prop_cwsNotificationEmails);
		
		sendEmail(subject, message, prop_cwsNotificationEmails.split(","));
	}
}

// custom appender or extend the SMPTAppender

