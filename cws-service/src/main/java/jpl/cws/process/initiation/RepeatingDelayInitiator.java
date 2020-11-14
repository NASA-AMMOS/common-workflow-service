package jpl.cws.process.initiation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;

/**
 * This is a reference implementation that demonstrates how
 * to create a custom process initiator.
 * 
 * This initiator has no practical purpose,
 * other than being an example.
 * 
 * @author ghollins
 *
 */
public class RepeatingDelayInitiator extends CwsProcessInitiator {
	private static final Logger log = LoggerFactory.getLogger(RepeatingDelayInitiator.class);
	
	private Long delayBetweenProcesses;
	private static final Long DEFAULT_DELAY_BETWEEN_PROCESSES = new Long(10000);
	private Long xtorDelayBetweenProcesses; // used for re-construction
	
	private Long maxRepeats;
	private static final long DEFAULT_MAX_REPEATS = 100;
	private Long xtorMaxRepeats;
	
	private int runNumber;
	
	public RepeatingDelayInitiator() {}  // needed by Spring for construction
	
	
	@Override
	public void run() {
		try {
			long numRuns = 0;
			while (++numRuns <= maxRepeats) {
				
				// Check whether initiator is still enabled
				if (!isEnabled()) {
					break; // stop running this initiator
				}
				
				// Create a variable for the process instance
				procVariables.put("runNumber", (runNumber++)+"");
				
				// set the initiationKey
				setInitiationKey("priority = " + procPriority + ", timer fire #" + numRuns);
				
				// schedule the process with the variable
				scheduleProcess();
				
				if (delayBetweenProcesses > 0) {
					// sleep before starting next loop iteration
					Thread.sleep(delayBetweenProcesses);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public MutablePropertyValues getSpecificPropertyValues() {
		log.debug("RepeatingDelayInitiator:  getSpecificPropertyValues()...");
		
		MutablePropertyValues propVals = new MutablePropertyValues();
		
		propVals.add("delayBetweenProcesses", xtorDelayBetweenProcesses);
		propVals.add("maxRepeats", xtorMaxRepeats);
		
		return propVals;
	}

	@Override
	public void reapplySpecificProps() {
		
		setDelayBetweenProcesses(xtorDelayBetweenProcesses);
		setMaxRepeats(xtorMaxRepeats);
	}
	
	@Override
	public boolean isValid() {
		return 
			delayBetweenProcesses >= 0
			&&
			maxRepeats >= 1;
	}

	public Long getDelayBetweenProcesses() {
		return delayBetweenProcesses;
	}
	public void setDelayBetweenProcesses(Long delayBetweenProcesses) {
		this.delayBetweenProcesses = this.xtorDelayBetweenProcesses = delayBetweenProcesses;
	}

	public Long getMaxRepeats() {
		return maxRepeats;
	}
	public void setMaxRepeats(Long maxRepeats) {
		this.maxRepeats = this.xtorMaxRepeats = maxRepeats;
	}

	public String toString() {
		return "RepeatingDelayInitiator ["+delayBetweenProcesses+"(" + xtorDelayBetweenProcesses+"), "+
				maxRepeats +" ("+xtorMaxRepeats+")] :: " + 
				super.toString();
	}
}

