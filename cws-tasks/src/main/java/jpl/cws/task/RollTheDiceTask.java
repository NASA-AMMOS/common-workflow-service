package jpl.cws.task;

import org.camunda.bpm.engine.delegate.Expression;

/**
 * Built-in task that simulates a roll of a dice, and sets an "outcome"
 * variable. The outcome is a zero-based number (i.e. dice sides start at 0, not
 * 1).
 * 
 * REQUIRED parameters:
 * 
 * OPTIONAL parameter: -- numSides (number of sides dice has; defaults to 2)
 * 
 */
public class RollTheDiceTask extends CwsTask {
	
	private static final Integer DEFAULT_NUM_SIDES = Integer.valueOf(2);

	private Expression numSides;
	private int numSidesInt;

	public RollTheDiceTask() {
		log.trace("RollTheDiceTask constructor...");
	}

	@Override
	public void initParams() throws Exception {
		numSidesInt = getIntegerParam(numSides, "numSides", DEFAULT_NUM_SIDES);
		if (numSidesInt < 0) {
			throw new IllegalArgumentException("negative numSides (" + numSidesInt + ")!");
		}
	}

	@Override
	public void executeTask() throws Exception {
		log.trace("in RollTheDiceTask::executeTask...");

		// Perform roll of the dice
		int outcome = (int) (System.currentTimeMillis() % (long) numSidesInt);
		log.debug("Dice roll came up " + outcome + " / " + numSidesInt);
		this.setOutputVariable("outcome", outcome);
	}

	public Expression getNumSides() {
		return numSides;
	}

	public void setNumSides(Expression numSides) {
		this.numSides = numSides;
	}
	
	public static void main(String args[]) {
		for (int i=0; i<1000; i++) {
			System.out.println( (int) (System.currentTimeMillis() % (long) 4));
		}
	}

}
