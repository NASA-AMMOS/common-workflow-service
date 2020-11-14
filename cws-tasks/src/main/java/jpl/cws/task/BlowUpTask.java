package jpl.cws.task;


/**
 * Built-in task that throws an exception (useful in simulating/testing
 * unexpected exceptions).
 * 
 * REQUIRED parameters:
 * 
 * OPTIONAL parameter:
 * 
 * 
 */
public class BlowUpTask extends CwsTask {

	public BlowUpTask() {
		log.trace("BlowUpTask constructor...");
	}

	@Override
	public void initParams() {
		// nothing to initialize
	}

	@Override
	public void executeTask() {
		log.trace("in BlowUpTask::executeTask... (about to blow up)");

		// always true -- avoid IDE warnings by doing it this way
		if (System.currentTimeMillis() > 0) {
			throw new NullPointerException("boom!! (simulated NPE)");
		}
	}

}
