package jpl.cws.task;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.DefaultAuthenticator;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.Expression;

/**
 * Built-in task that sends and email.
 * 
 * REQUIRED parameters: -- from -- to -- subject -- body
 * 
 * OPTIONAL parameters: -- smtpHost (defaults to "localhost") -- smtpPort
 * (defaults to 25)
 * 
 */
public class EmailTask extends CwsTask {
	
	private static final String DEFAULT_FROM = "cws_admin@localhost";
	private static final String BODY_TYPE_HTML = "html";
	private static final String DEFAULT_BODY_TYPE = BODY_TYPE_HTML;
	private static final String DEFAULT_SMTP_HOST = "smtp.localhost";
	private static final Integer DEFAULT_SMTP_PORT = Integer.valueOf("25");
	private static final String MAIL_FAILURE = "mailFailure";

	private Expression from;
	private Expression authuser;
	private Expression authpwd;
	private Expression to;
	private Expression subject;
	// either 'html' or 'text'
	private Expression bodyType;
	private Expression body;
	private Expression smtpHost;
	private Expression smtpPort;
	private String fromString;
	private String authuserString;
	private String authpwdString;
	private String toString;
	private String subjectString;
	private String bodyTypeString;
	private String bodyString;
	private String smtpHostString;
	private int smtpPortInt;

	public EmailTask() {
		log.trace("EmailTask constructor...");
	}

	@Override
	public void initParams() throws Exception {
		fromString = getStringParam(from, "from", DEFAULT_FROM);
		authuserString = getStringParam(authuser, "authUser", null);
		authpwdString = getStringParam(authpwd, "authPwd", null);
		toString = getStringParam(to, "to");
		subjectString = getStringParam(subject, "subject", null);
		bodyTypeString = getStringParam(bodyType, "bodyType", DEFAULT_BODY_TYPE);
		bodyString = getStringParam(body, "body", null);
		smtpHostString = getStringParam(smtpHost, "smtpHost", DEFAULT_SMTP_HOST);
		smtpPortInt = getIntegerParam(smtpPort, "smtpPort", DEFAULT_SMTP_PORT);
	}

	/**
	 * Base implementation of sending an email
	 * 
	 */
	@Override
	public void executeTask() {

		log.info("Sending email...");

		try {
			Email email;

			if (bodyTypeString.equals(BODY_TYPE_HTML)) {
				email = new HtmlEmail();

				// set the html message
				((HtmlEmail) email).setHtmlMsg("<html>"
						+ bodyString.replaceAll("\\n", "<br/>").replaceAll("&#10;", "<br/>") + "</html>");
				// set the alternative text message
				((HtmlEmail) email).setTextMsg("TEXT ALTERNATIVE: " + bodyString.replaceAll("&#10;", "\n"));
			} else {
				email = new SimpleEmail();

				// set the plain text message
				((SimpleEmail) email).setMsg(bodyString);
			}



			email.setHostName(smtpHostString);
			email.setSmtpPort(smtpPortInt);
			email.setFrom(fromString);
			email.setSubject(subjectString);
			email.setAuthenticator(new DefaultAuthenticator(authuserString, authpwdString));

			String[] recipients = toString.split(",");
			for (String recip : recipients) {
				email.addTo(recip.trim());
			}

			// Send email
			//
			log.info("About to send email to " + toString + " ...");
			email.send();
			this.setOutputVariable("emailSendStatus", "success");
		} catch (Throwable e) {
			log.error("Problem occurred while sending email", e);
			this.setOutputVariable("emailSendStatus", "failure");
			throw new BpmnError(MAIL_FAILURE);
		}
	}

	public Expression getFrom() {
		return from;
	}

	public void setFrom(Expression from) {
		this.from = from;
	}

	public Expression getAuthUser() { return authuser; }

	public void setAuthUser(Expression authUser) { this.authuser = authuser; }

	public Expression getTo() {
		return to;
	}

	public void setTo(Expression to) {
		this.to = to;
	}

	public Expression getSubject() {
		return subject;
	}

	public void setSubject(Expression subject) {
		this.subject = subject;
	}

	public Expression getBodyType() {
		return bodyType;
	}

	public void setBodyType(Expression bodyType) {
		this.bodyType = bodyType;
	}

	public Expression getBody() {
		return body;
	}

	public void setBody(Expression body) {
		this.body = body;
	}

	public Expression getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(Expression smtpHost) {
		this.smtpHost = smtpHost;
	}

	public Expression getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(Expression smtpPort) {
		this.smtpPort = smtpPort;
	}

}
