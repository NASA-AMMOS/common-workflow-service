package jpl.cws.process.initiation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.db.SchedulerJob;
import jpl.cws.scheduler.Scheduler;

/**
 * Abstract super class that all internal CWS initiators must extend.
 * 
 * @author ghollins
 *
 */
public abstract class CwsProcessInitiator extends Thread implements BeanNameAware, InitializingBean {
	
	private static final Logger log = LoggerFactory.getLogger(CwsProcessInitiator.class);
	
	@Autowired protected Scheduler cwsScheduler;
	@Autowired protected SchedulerDbService cwsSchedulerDbService;
	
	protected String procDefKey;
	protected String xtorProcDefKey; // keep track of initial xtor value, for re-construction later
	
	protected Map<String,String> procVariables;
	protected Map<String,String> xtorProcVariables; // keep track of initial xtor value, for re-construction later
	
	protected String procBusinessKey;
	protected String xtorProcBusinessKey; // keep track of initial xtor value, for re-construction later
	
	private String initiationKey;
	protected String xtorInitiationKey; // keep track of initial xtor value, for re-construction later
	
	private static final String DEFAULT_PROC_PRIORITY = "10";
	protected String procPriority = DEFAULT_PROC_PRIORITY;
	protected String xtorProcPriority; // keep track of initial xtor value, for re-construction later
	
	protected String initiatorId;
	protected boolean enabled = false; // disabled by default
	
	protected boolean valid = false;
	protected String invalidMsg = "";
	
	
	protected static Random random = new Random();
	
	public CwsProcessInitiator() {
		log.trace("CwsProcessInitiator default xtor...");
		// procVariables is empty at first / by default
		this.procVariables = this.xtorProcVariables = new HashMap<String,String>();
	}
	
	
	/**
	 * Schedule the process (with the current set of member variables)
	 * 
	 */
	protected SchedulerJob scheduleProcess() throws Exception {
		return cwsScheduler.scheduleProcess(
			procDefKey,
			procVariables,
			procBusinessKey,
			initiationKey,
			Integer.parseInt(procPriority)
		);
	}
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.trace("CwsProcessInitiator::afterPropertiesSet()...");
		
		// Run sub-class isValid implementation
		//
		valid = isValid();
		
		// Validate that procDefKey is set
		//
		if (procDefKey == null || procDefKey.isEmpty()) {
			log.error("procDef is not specified (procDefKey = " + procDefKey + ")!");
			invalidMsg += "procDef is not specified (procDefKey = " + procDefKey + ")!";
			valid = false;
		}
	
		// Log validity status
		//
		String name = this.getClass().getSimpleName();
		if (!valid) {
			log.warn("CwsProcessInitiator " + name + " (" + initiatorId + ") is NOT valid!");
		}
		else {
			log.trace("CwsProcessInitiator " + name + " (" + initiatorId + ") is valid");
		}
	}
	
	
	/**
	 * Sub-classes of CwsProcessInitiator must implement this method.
	 */
	public abstract boolean isValid();
	
	
	/**
	 * Sub-classes of CwsProcessInitiator must implement this method.
	 * 
	 * This holds this logic that runs when the initiator is enabled.
	 * 
	 */
	public abstract void run();
	
	
	/**
	 * Sub-classes of CwsProcessInitiator must implement this method.
	 * 
	 * Returns the specific set of property values for the child
	 * implementation of the initiator.
	 * 
	 * Shared values, common to all initiators are provided by the 
	 * getPropertyValues method below.
	 * 
	 */
	public abstract MutablePropertyValues getSpecificPropertyValues();
	
	/**
     * Sub-classes of CwsProcessInitiator must implement this method.
     * 
     * This re-applies the properties of this bean.
     * 
     */
    public abstract void reapplySpecificProps();
    
	/**
	 * Returns the complete set of property values that should be
	 * set immediately after this object is constructed.
	 * 
	 */
	public MutablePropertyValues getPropertyValues() {
		log.trace("CwsProcessInitiator:  getPropertyValues()...");
		
		MutablePropertyValues propVals = new MutablePropertyValues();
		
		// Set values shared by all initiators
		//
		propVals.add("procDefKey",      xtorProcDefKey);
		propVals.add("procVariables",   xtorProcVariables);
		propVals.add("procBusinessKey", xtorProcBusinessKey);
		propVals.add("initiationKey",   xtorInitiationKey);
		propVals.add("procPriority",    xtorProcPriority);
		
		// Add in specific values from the child initiator implementation
		//
		propVals.addPropertyValues(getSpecificPropertyValues());
		
		return propVals;
	}
	
	public void reapplyProps() {
        setProcDefKey(xtorProcDefKey);
        setProcVariables(xtorProcVariables);
        setProcBusinessKey(xtorProcBusinessKey);
        setInitiationKey(xtorInitiationKey);
        setProcPriority(xtorProcPriority);
        
        reapplySpecificProps();
    }
	
	public String getType() {
		return this.getClass().getSimpleName();
	}
	
	
	public String getProcDefKey() { return this.procDefKey; }
	public void setProcDefKey(String procDefKey) { this.procDefKey = this.xtorProcDefKey = procDefKey; }
	
	public Map<String,String> getProcVariables() { return procVariables; }
	public void setProcVariables(Map<String,String> procVariables) { this.procVariables = this.xtorProcVariables = procVariables; }
	
	public String getProcBusinessKey() { return procBusinessKey; }
	public void setProcBusinessKey(String procBusinessKey) { this.procBusinessKey = this.xtorProcBusinessKey = procBusinessKey; }
	
	public String getInitiationKey() { return initiationKey; }
	public void setInitiationKey(String initiationKey) {
		this.initiationKey = this.xtorInitiationKey = initiationKey;
	}
	
	public String getProcPriority() { return procPriority; }
	public void setProcPriority(String procPriority) {
		//
		// Convert priority String to int or randomized int
		//
		if (procPriority != null) {
			if (procPriority.toString().equalsIgnoreCase("rand") ||
					procPriority.toString().equalsIgnoreCase("random")) {
				this.procPriority = this.xtorProcPriority = (random.nextInt(20) +1)+""; // number between 1 and 20
			}
			else {
				try {
					Integer.parseInt(procPriority);
					this.procPriority = this.xtorProcPriority = procPriority; // valid integer if we got here
				} catch (Exception e) {
					log.error("Problem parsing priority '" + procPriority +
							"'. Setting priority to default value of " + DEFAULT_PROC_PRIORITY);
					this.procPriority = this.xtorProcPriority = DEFAULT_PROC_PRIORITY;
				}
			}
		}
	}
	
	public String getInitiatorId() {
		return this.initiatorId;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void cleanUp() {
		
	}
	
	public String getStatus() {
		if (enabled) { return "enabled"; }
		else { return "disabled"; }
	}
	
	public String getInvalidMsg() {
		return invalidMsg;
	}
	
	public String toString() {
		return 
			" (initiatorId='"      + getInitiatorId() + "'" +
			", enabled="           + isEnabled() +
			", procDefKey='"       + getProcDefKey() + "'" +
			", procPriority="      + getProcPriority() +
			", procBusinessKey='"  + getProcBusinessKey() + "'" +
			", initiationKey='"    + getInitiationKey() + "'" +
			", procVariables="     + getProcVariables() +
			", valid="           + valid +
			")";
	}
	
	
	@Override
	public void setBeanName(String name) {
		this.initiatorId = name;
	}

}
