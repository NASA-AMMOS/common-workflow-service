package jpl.cws.process.initiation.cron;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.scheduler.Scheduler;

public class RunProcDefJob implements Job {

	private static Logger log = LoggerFactory.getLogger(RunProcDefJob.class);

	/**
	 * Quartz requires a public empty constructor so that the
	 * scheduler can instantiate the class whenever it needs.
	 */
	public RunProcDefJob() {
	}

	/**
	 * <p>
	 * Called by the <code>{@link org.quartz.Scheduler}</code> when a
	 * <code>{@link org.quartz.Trigger}</code> fires that is associated with
	 * the <code>Job</code>.
	 * </p>
	 * 
	 * @throws JobExecutionException
	 *             if there is an exception while executing the job.
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException {

		try {
			JobKey jobKey = context.getJobDetail().getKey();
			log.info("Cron Job: " + jobKey + " executing at " + new Date());
			
			JobDataMap data = context.getMergedJobDataMap();
			
			String procDefKey = data.getString("procDefKey");
			Scheduler cwsScheduler = (Scheduler)data.get("cwsScheduler");
			
			// Get procVars from JobDataMap. This is a JSON string and
			// is parsed by Gson to re-create the original map
			String procVarsJsonString = data.getString("procVariables");
			Gson gson = new GsonBuilder().create();
			Type typeOfHashMap = new TypeToken<Map<String, String>>() {}.getType();
			Map<String, String> processVariables = gson.fromJson(procVarsJsonString, typeOfHashMap);
			
			// Set to default priority
			cwsScheduler.scheduleProcess(procDefKey, processVariables, null, null, 10);
		}
		catch (Exception e) {
			log.error("CronInitiator failed to execute Job.  Details: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
