package jpl.cws.task;

import org.camunda.bpm.engine.delegate.Expression;

/**
 * Built-in task that moves a file from one location on the file system to
 * another.
 * 
 * REQUIRED parameters: -- message
 * 
 */
public class LogTask extends CwsTask {
	private static final String DEFAULT_MESSAGE = "You forgot to specify a message to log!";

	private Expression message;

	private String messageString;

	public LogTask() {
		log.trace("LogTask constructor...");
	}

	@Override
	public void initParams() throws Exception {
		messageString = getStringParam(message, "message", DEFAULT_MESSAGE);
	}

	@Override
	public void executeTask() throws Exception {
		// Log the message
		log.info(messageString);

		this.setOutputVariable("message", messageString);
	}

	public Expression getMessage() {
		return message;
	}

	public void setMessage(Expression message) {
		this.message = message;
	}
}
