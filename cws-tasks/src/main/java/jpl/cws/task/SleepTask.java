package jpl.cws.task;

import org.camunda.bpm.engine.delegate.Expression;

/**
 * Built-in task that sleeps for a period of time.
 * 
 * REQUIRED parameters:
 * 
 * OPTIONAL parameter: -- duration (defaults to 10 seconds) -- numLogs
 * 
 */
public class SleepTask extends CwsTask {

	// 10 seconds
	private final static long DEFAULT_DURATION = 10000;
	private final static int DEFAULT_NUM_LOGS = 5;

	private Expression duration;
	private Expression numLogs;
	private long durationLong;
	private int numLogsInt;

	public SleepTask() {
		log.trace("SleepTask constructor...");
	}

	@Override
	public void initParams() throws Exception {
		durationLong = getLongParam(duration, "duration", DEFAULT_DURATION);
		if (durationLong < 0) {
			throw new IllegalArgumentException("negative duration (" + durationLong + ")!");
		}

		numLogsInt = getIntegerParam(numLogs, "numLogs", DEFAULT_NUM_LOGS);
		if (numLogsInt < 0) {
			throw new IllegalArgumentException("negative numLogs (" + numLogsInt + ")");
		}
	}

	@Override
	public void executeTask() throws Exception {
		log.trace("in SleepTask::executeTask...");

		// Perform sleep
		try {
			long durationBetweenLogs = durationLong / numLogsInt;
			if (durationBetweenLogs >= 999) {
				// log if frequency is ~1 second or more
				for (int i = 0; i < numLogsInt; i++) {
					log.info("Sleeping... (" + (i * durationBetweenLogs) + " out of " + durationLong + " ms)");
					Thread.sleep(durationBetweenLogs);
				}
			} else {
				log.info("Sleeping for " + durationLong + " ms...");
				Thread.sleep(durationLong);
			}
		} catch (InterruptedException e) {
			log.error("interrupted", e);
		}

		this.setOutputVariable("sleptFor", durationLong + "");
	}

	public Expression getDuration() {
		return duration;
	}

	public void setDuration(Expression duration) {
		this.duration = duration;
	}

	public Expression getNumLogs() {
		return numLogs;
	}

	public void setNumLogs(Expression numLogs) {
		this.numLogs = numLogs;
	}

}
