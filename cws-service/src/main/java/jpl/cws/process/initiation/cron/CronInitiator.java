package jpl.cws.process.initiation.cron;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;

import jpl.cws.process.initiation.CwsProcessInitiator;

public class CronInitiator extends CwsProcessInitiator {
	
	private static final Logger log = LoggerFactory.getLogger(CronInitiator.class);
	
	private String cronExpression;
	private String xtorCronExpression;
	
	
	public CronInitiator() {} // needed by Spring for construction
	
	
	@Override
	public boolean isValid() {
		if (cronExpression == null || cronExpression.isEmpty()) {
			log.error("Must specify a cron expression string!");
			return false;
		}
		
		if (!CronExpression.isValidExpression(cronExpression)) {
			log.error("cronExpression is not a valid cron expression string!");
			return false;
		}
		
		return true;
	}
	
	
	@Override
	public void run() {
		try {
			
			log.info("------------------- Cron Initiator run() Started -------------------");
			log.info("initiatorId = " + initiatorId);
			log.info("procDefKey = " + procDefKey);
			log.info("cronExpression = " + cronExpression);
			
			// First we must get a reference to a scheduler
			SchedulerFactory sf = new StdSchedulerFactory();
			Scheduler sched = sf.getScheduler();
			
			// Saving data for Job thread
			JobDataMap jobDataMap = new JobDataMap();
			
			jobDataMap.put("procDefKey", procDefKey);
			jobDataMap.put("cwsScheduler", cwsScheduler);

			// Convert procVariables map to a json string
			// This necessary because Quartz JobDataMap only allows primitive values
			Gson gson = new GsonBuilder().create();
			String procVarsJsonString = gson.toJson(procVariables);
			jobDataMap.put("procVariables", procVarsJsonString);
			
			// Schedule new job
			JobDetail job = newJob(RunProcDefJob.class).withIdentity(initiatorId + "-Job", "group1").usingJobData(jobDataMap).build();
			
			CronTrigger trigger = newTrigger().withIdentity(initiatorId + "-Trigger", "group1").withSchedule(cronSchedule(cronExpression)).build();
			
			Date ft = sched.scheduleJob(job, trigger);
			
			log.info(job.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
					+ trigger.getCronExpression());
				
			log.info("------- Starting Cron Scheduler ----------------");
				
			sched.start();
				
			log.info("------- Started Scheduler -----------------");
			
			try {
				
				while (true) {
					
					// wait five minutes (sleep until interrupted when initiator is turned OFF)
					Thread.sleep(300L * 1000L);
				}
			} catch (Exception e) {}
			
			// This initiator was turned OFF so delete this job and trigger from the scheduler
			sched.deleteJob(job.getKey());
			
			if (!anyJobsScheduled(sched)) {
				
				log.info("------- Cron Scheduler Going Into Standby ---------------------");
				
				sched.standby();
				
				log.info("------- Standby Complete -----------------");
				
				SchedulerMetaData metaData = sched.getMetaData();
				log.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
			}
		}
		catch (Exception e) {
			
			log.error("CronInitiator '" + initiatorId + "' run() failed!  Details: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	private Boolean anyJobsScheduled(Scheduler scheduler) {
		
		try {
			for (String groupName : scheduler.getJobGroupNames()) {
				
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
					
					log.info("Still running cron Job: " + jobKey);
					
					return true;
				}
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	
	@Override
	public MutablePropertyValues getSpecificPropertyValues() {
		MutablePropertyValues propVals = new MutablePropertyValues();
		
		propVals.add("cronExpression", xtorCronExpression);
		
		return propVals;
	}
	
	
	public String getCronExpression() {
		return cronExpression;
	}
	public void setCronExpression(String cronExpression) {
		this.cronExpression = this.xtorCronExpression = cronExpression;
	}


	@Override
	public void reapplySpecificProps() {
		setCronExpression(xtorCronExpression);
	}
	
}
