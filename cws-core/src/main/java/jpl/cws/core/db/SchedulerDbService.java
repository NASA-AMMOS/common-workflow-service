package jpl.cws.core.db;

import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;

import de.ruedigermoeller.serialization.FSTObjectOutput;
import jpl.cws.core.log.CwsEmailerService;

/**
 * Helper / service methods related scheduler database tables.
 * 
 * @author ghollins
 *
 */
public class SchedulerDbService extends DbService implements InitializingBean {
	private static int externalWorkerNum = 0;
	private static final Logger log = LoggerFactory.getLogger(SchedulerDbService.class);
	
	@Autowired private CwsEmailerService cwsEmailerService;
	
	public static final String PENDING            = "pending";
	public static final String DISABLED           = "disabled";
	public static final String FAILED_TO_SCHEDULE = "failedToSchedule";
	public static final String CLAIMED_BY_WORKER  = "claimedByWorker";
	public static final String FAILED_TO_START    = "failedToStart";
	public static final String RUNNING            = "running";
	public static final String COMPLETE           = "complete";
	public static final String RESOLVED           = "resolved";
	public static final String FAIL               = "fail";
	public static final String INCIDENT			  = "incident";
	
	public static final int DEFAULT_WORKER_PROC_DEF_MAX_INSTANCES = 1;
	public static final int PROCESSES_PAGE_SIZE = 100;
	
	// KEY FOR THIS IS:  KEY `claimKey` (`status`,`proc_def_key`,`priority`,`created_time`)
	public static final String FIND_CLAIMABLE_ROWS_SQL = 
			"SELECT uuid FROM cws_sched_worker_proc_inst " +
			"WHERE " +
			"  status='"+PENDING+"' AND " +
			"  proc_def_key=? " +
			"ORDER BY " +
			"  priority ASC, " +     // lower priorities    favored
			"  created_time ASC " +  // older dates (FIFO)  favored
			"LIMIT ?";

	public static final String UPDATE_CLAIMABLE_ROW_SQL = 
			"UPDATE cws_sched_worker_proc_inst " +
			"SET " +
			"  claimed_by_worker=?, " +
			"  claim_uuid=?, " +
			"  status='"+CLAIMED_BY_WORKER+"' " +
			"WHERE " +
			"  uuid=? AND claim_uuid IS NULL " +
			"  AND EXISTS (SELECT * FROM cws_worker WHERE id=? AND status='up')";
	
	public static final String INSERT_SCHED_WORKER_PROC_INST_ROW_SQL = 
			"INSERT INTO cws_sched_worker_proc_inst " +
			"(uuid, created_time, updated_time, proc_inst_id, " +
			"proc_def_key, proc_business_key, priority, proc_variables, status, error_message, " +
			"initiation_key, claimed_by_worker, started_by_worker, last_rejection_worker, num_worker_attempts, claim_uuid) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public static final String PROC_INST_STATUS_SQL =
		" IF (PI.END_TIME_ IS NULL, 'running', " +
			"IF (AI.ACT_TYPE_ in ('noneEndEvent','endEvent','escalationEndEvent','compensationEndEvent','signalEndEvent','terminateEndEvent') AND " +
			"PI.END_TIME_ IS NOT NULL, 'complete', 'fail')) ";

	
	public SchedulerDbService() {
		log.trace("SchedulerDbService constructor...");
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.trace("jdbcTemplate = "+jdbcTemplate);
	}
	
	
	public boolean engineProcessRowExists(String procDefKey) {
		int numRows = jdbcTemplate.queryForObject(
			"SELECT count(*) FROM cws_worker_proc_def " +
			"WHERE proc_def_key=?",
			new Object[]{procDefKey}, Integer.class);
		return numRows > 0;
	}
	
	
	/**
	 * Inserts a row into the cws_sched_worker_proc_inst table.
	 * 
	 */
	public void insertSchedEngineProcInstRow(final SchedulerJob schedulerJob) throws Exception {
		long t0 = System.currentTimeMillis();

		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			FSTObjectOutput out = new FSTObjectOutput(os);
			out.writeObject(schedulerJob.getProcVariables());
			out.close();

			DefaultLobHandler lobHandler = new DefaultLobHandler();
			Object o = jdbcTemplate.execute(
					INSERT_SCHED_WORKER_PROC_INST_ROW_SQL,
					new AbstractLobCreatingPreparedStatementCallback(lobHandler) {
						protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException {
							ps.setString(1, schedulerJob.getUuid());
							ps.setTimestamp(2, schedulerJob.getCreatedTime()); // will get auto-filled by DB
							ps.setTimestamp(3, schedulerJob.getUpdatedTime()); // will get auto-filled by DB
							ps.setString(4, null); // don't know process instance ID yet
							ps.setString(5, schedulerJob.getProcDefKey());
							ps.setString(6, schedulerJob.getProcBusinessKey());
							ps.setInt(7, schedulerJob.getProcPriority());
							lobCreator.setBlobAsBytes(ps, 8, os.toByteArray());
							ps.setString(9, schedulerJob.getStatus());
							ps.setString(10, null); // no error message yet
							ps.setString(11, schedulerJob.getInitiationKey());
							ps.setString(12, null); // no claimed_by_worker yet
							ps.setString(13, null); // not started on any worker yet
							ps.setString(14, null); // no worker rejections yet
							ps.setInt(15, 0); // no worker attempts yet
							ps.setString(16, null); // no claim_uuid yet
						}
					}
			);
			log.trace("RETURN OBJECT: " + o);
			o = null;
			long timeTaken = System.currentTimeMillis() - t0;
			if (timeTaken > SLOW_WARN_THRESHOLD) {
				log.warn("INSERT INTO cws_sched_worker_proc_inst took " + timeTaken + " ms!");
			}
		}
	}
	
	
	public void batchUpdateProcInstRowStatus(
			Set<String> uuids,
			String oldStatus,
			String newStatus) throws Exception {
		
		log.warn("batch is " + uuids + ", " + uuids.size());
	}
	
	
	/**
	 * Updates a cws_sched_worker_proc_inst row's status,
	 * while ensuring only a valid state transition occurs
	 * (by querying by current/old status).
	 * 
	 */
	public void updateProcInstRowStatus(
		String uuid,
		String oldStatus,
		String newStatus,
		String errorMessage,
		boolean clearWorkerInfo) throws Exception {
		long t0 = System.currentTimeMillis();
		log.trace("uuid="+uuid+":  " + oldStatus + "--->" + newStatus +", errorMessage="+errorMessage);
		
		// Attempt to update the database
		// There is a slight chance that the process will finish so quickly that
		// we may need to try several times here, hence the while loop.
		// The startEvent of the process should asynchronous
		// (see issue #188, and "Creating CWS Compliant BPMN" section in User's Guide)
		// to avoid race conditions here
		int numTries = 0;
		int numUpdated = 0;
		
		while (numUpdated == 0 && numTries < 20) {
			numUpdated = jdbcTemplate.update(
				"UPDATE cws_sched_worker_proc_inst " +
				"SET status=?, updated_time=?, " +
				(clearWorkerInfo ? "claim_uuid = NULL, claimed_by_worker = NULL, started_by_worker = NULL, last_rejection_worker = NULL," : "") +
				"error_message=? " +
				"WHERE uuid=? AND status=?",
				new Object[] {newStatus,
						new Timestamp(DateTime.now().getMillis()),
						errorMessage, uuid, oldStatus});
			if (numUpdated == 0 && ++numTries < 20) {
				String rowStatus = getProcInstRowStatus(uuid);
				
				// Workaround for potential Camunda bug.
				//  This bug should now be fixed (as of v7.3.1+),
				//  but it was decided that leaving this code in anyways
				//  is probably a good safeguard.
				//    See: https://groups.google.com/forum/#!topic/camunda-bpm-users/nFlmxFaKngM
				//    See: https://app.camunda.com/jira/browse/CAM-2937
				//
				if (COMPLETE.equals(rowStatus) && newStatus.equals(rowStatus)) {
					log.warn("already updated row to '"+COMPLETE+"' status -- workaround for Camunda bug");
					return; // don't try to update anymore
				}
				
				log.warn("sleeping before trying DB update again...");
				Thread.sleep(250);
			}
		}
		long timeTaken = System.currentTimeMillis() - t0;
		if (timeTaken > SLOW_WARN_THRESHOLD) {
			log.warn("updateProcInstRowStatus (cws_sched_worker_proc_inst) took " + timeTaken + " ms!");
		}
		
		if (numUpdated != 1) {
			throw new Exception("did not update 1 row, updated "+numUpdated + ". " + 
					"(uuid="+uuid+":  " + oldStatus + "--->" + newStatus +", errorMessage="+errorMessage+")");
		}
	}
	
	
	public int updateProcInstIdAndStartedByWorker(
			String uuid,
			String workerId,
			String procInstId) throws Exception {
		long t0 = System.currentTimeMillis();
		int numUpdated = jdbcTemplate.update(
				"UPDATE cws_sched_worker_proc_inst " +
				"SET started_by_worker=?, proc_inst_id=?, updated_time=? " +
				"WHERE uuid=?",
				new Object[] {
					workerId,
					procInstId,
					new Timestamp(DateTime.now().getMillis()),
					uuid}
				);
		long timeTaken = System.currentTimeMillis() - t0;
		if (timeTaken > SLOW_WARN_THRESHOLD) {
			log.warn("UPDATE of cws_sched_worker_proc_inst took " + timeTaken + " ms!");
		}
		return numUpdated;
	}
	
	
	/**
	 * Attempt to claim a process start request in the database.
	 * 
	 * @param limitToTheseProcessDefs -- if specified, only attempt to claim these types of process defs
	 * @return number of rows claimed
	 */
	public Map<String,List<String>> claimHighestPriorityStartReq(String workerId, List<String> limitToTheseProcessDefs, String procDefKey, int limit) {
		List<String> claimUuids = new ArrayList<String>();
		List<String> rowUuids = new ArrayList<String>();
		List<String> claimedRowUuids = new ArrayList<String>();
		long t0 = System.currentTimeMillis();
		int numUpdated = 0;
		String claimUuid = null;
		if (procDefKey == null) {
			log.error("procDefKey is null!! code should not reach here");
		}
		else {
			// Try to update, until we succeed.
			//
			int attempts = 0;
			
			while (attempts++ < 10) {
				// ---------------------------------
				// SELECT then UPDATE methodology
				// ---------------------------------
				try {
					// SELECT CANDIDATE ROWS
					//
					rowUuids = jdbcTemplate.queryForList(
							FIND_CLAIMABLE_ROWS_SQL, String.class,
							new Object[] {procDefKey, limit});
					
					if (!rowUuids.isEmpty()) {
						// Iterate over candidates, trying to update them
						//
						for (String uuid : rowUuids) {
							claimUuid = UUID.randomUUID().toString();
							int updateCount = jdbcTemplate.update(UPDATE_CLAIMABLE_ROW_SQL,
									new Object[] {workerId, claimUuid, uuid, workerId});
							
							// FIXME:  IS THERE A WAY TO MAKE THIS DO BATCH UPDATES, INSTEAD OF ONE AT A TIME??

							if (updateCount == 1) {
								numUpdated++;
								claimUuids.add(claimUuid);
								claimedRowUuids.add(uuid);
								log.debug("CLAIMED " + claimUuid + " (uuid=" +uuid+")");
							}
							//else {
							//	log.info("DID NOT CLAIM " + claimUuid + " (uuid=" +uuid+")");
							//}
						}
						//log.debug("updated " + numUpdated + " / " + rowUuids.size());
					}
					else if (log.isTraceEnabled()) {
						log.trace("NO CLAIMABLE CANDIDATES AT THIS TIME");
					}
					
					break; // no retry needed
				}
				catch (DeadlockLoserDataAccessException e) {
					if (attempts == 10) {
						log.error("Caught a DeadlockLoserDataAccessException.  NOT Retyring as 10 attempts have been tried already!..");
						break; // give up
					}
					log.warn("Caught a DeadlockLoserDataAccessException.  Retrying..");
					continue; // retry
				}
				catch (Throwable t) {
					log.error("Unexpected exception.  Not retrying..", t);
					break; // abort
				}
			} // end while (attempts)
		}
		long timeTaken = System.currentTimeMillis() - t0;
		if (timeTaken > SLOW_WARN_THRESHOLD) {
			log.warn("CLAIM cws_sched_worker_proc_inst took " + timeTaken + " ms!");
		}
		if (numUpdated >= 1) {
			log.info("worker " + workerId + " claimed " + numUpdated + " row(s).");
		}
		else {
			log.trace("no rows claimed by worker: " + workerId);
		}
		
		if (numUpdated != claimUuids.size()) {
			log.error("numUpdated != claimUuids.size()" );
		}
		
		Map<String,List<String>> ret = new HashMap<String,List<String>>();
		ret.put("claimUuids", claimUuids);
		ret.put("claimedRowUuids", claimedRowUuids);
		
		return ret;
	}
	
	
	public String getProcInstRowStatus(String uuid) {
		List<Map<String,Object>> list = jdbcTemplate.queryForList(
			"SELECT status FROM cws_sched_worker_proc_inst " +
			"WHERE uuid=?",
			new Object[] {uuid});
		if (list != null && !list.isEmpty()) {
			return list.iterator().next().values().iterator().next().toString();
		}
		else {
			return null;
		}
	}
	
	
	public Map<String,Object> getProcInstRow(String uuid) {
		List<Map<String,Object>> list = jdbcTemplate.queryForList(
			"SELECT * FROM cws_sched_worker_proc_inst " +
			"WHERE uuid=?",
			new Object[] {uuid});
		if (list.size() != 1) {
			log.error("unexpected list size: " + list.size() + ", for uuid: " + uuid);
		}
		if (list != null && !list.isEmpty()) {
			return list.iterator().next();
		}
		else {
			return null;
		}
	}
	
	
	/**
	 * 
	 */
	public List<Map<String,Object>> getClaimedProcInstRows(List<String> claimUuids) {
		long t0 = System.currentTimeMillis();
		String claimUuidsStr = "'" + StringUtils.join(claimUuids.toArray(), "','") + "'";
		List<Map<String,Object>> list = jdbcTemplate.queryForList(
			"SELECT * FROM cws_sched_worker_proc_inst " +
			"WHERE claim_uuid IN (" + claimUuidsStr + ")");
			//new Object[] {claimUuid});
		long timeTaken = System.currentTimeMillis() - t0;
		if (timeTaken > SLOW_WARN_THRESHOLD) {
			log.warn("SELECT * FROM cws_sched_worker_proc_inst by claim_uuid took " + timeTaken + " ms!");
		}
		
		if (list.size() != claimUuids.size()) {
			log.error("unexpected claim size: " + list.size() + ", for claim_uuids: " + claimUuidsStr +
					" (expected " + claimUuids.size() + ")");
		}
		return list;
	}
	

	public boolean externalWorkerExists(String workerId) {
		return jdbcTemplate.queryForObject(
			"SELECT count(*) FROM cws_external_worker WHERE id=?", new Object[]{workerId}, Integer.class) > 0;
	}
		
	/**
	 * Create a row (if not already exists) in the database for this engine
	 */
	public String createExternalWorkerRow(String workerId, String hostname) {		
		if (!externalWorkerExists(workerId)) {
			log.info("Inserting row into cws_external_worker table...");
			
			int numUpdated = 0;
			int numTries = 0;
			String workerName = null;
			while (numTries++ < 10 && numUpdated != 1) {
				Timestamp tsNow = new Timestamp(DateTime.now().getMillis());
				workerName = "ext_worker" + String.format("%1$4s", externalWorkerNum++).replace(' ', '0');
				
				try {
					numUpdated = jdbcTemplate.update(
							"INSERT INTO cws_external_worker" +
							"   (id, name, hostname, created_time, last_heartbeat_time) " +
							"VALUES (?,?,?,?,?)", 
							new Object[] {
									workerId,
									workerName,
									hostname,
									tsNow, // created_time
									tsNow  // last_heartbeat_time
							});
				}
				catch (DataAccessException e) {
					
					try {
						// Could not update database, wait and retry again
						Thread.sleep((long)(Math.random() * 500.0));
					}
					catch (InterruptedException ex) {
						
					}
				}
			}
			
			if (numUpdated != 1) {
				log.error("Could not create external worker row for workerId " + workerId + " !");
			}
			
			return workerName;
		}
		
		log.error("Could not create external worker row for workerId " + workerId + " !");
		
		return null;
	}
	
	
	public int updateExternalWorkerHeartbeat(String workerId) {
		return jdbcTemplate.update(
			"UPDATE cws_external_worker SET last_heartbeat_time = ? WHERE id=?",
			new Object[] { new Timestamp(DateTime.now().getMillis()), workerId }
		);
	}
	
	public int updateExternalWorkerActiveTopics(String workerId, String activeTopics) {
		return jdbcTemplate.update(
			"UPDATE cws_external_worker SET activeTopics = ? WHERE id=?",
			new Object[] { activeTopics, workerId }
		);
	}
	
	public int updateExternalWorkerCurrentTopic(String workerId, String currentTopic) {
		return jdbcTemplate.update(
			"UPDATE cws_external_worker SET currentTopic = ? WHERE id=?",
			new Object[] { currentTopic, workerId }
		);
	}
	
	public int updateExternalWorkerCurrentCommand(String workerId, String currentCommand) {
		return jdbcTemplate.update(
			"UPDATE cws_external_worker SET currentCommand = ? WHERE id=?",
			new Object[] { currentCommand, workerId }
		);
	}
	
	public int updateExternalWorkerCurrentWorkingDir(String workerId, String currentWorkingDir) {
		return jdbcTemplate.update(
			"UPDATE cws_external_worker SET currentWorkingDir = ? WHERE id=?",
			new Object[] { currentWorkingDir, workerId }
		);
	}
	
	public List<Map<String,Object>> getWorkers() {
		return jdbcTemplate.queryForList(
			"SELECT * FROM cws_worker ORDER BY name");
	}
	
	public List<Map<String,Object>> getExternalWorkers() {
		return jdbcTemplate.queryForList(
			"SELECT * FROM cws_external_worker ORDER BY name");
	}
	
	public List<Map<String,Object>> getWorkersStats() {
		return jdbcTemplate.queryForList(
			"SELECT status, COUNT(*) as cnt FROM cws_worker WHERE cws_install_type != 'console_only' GROUP BY status");
	}
	
	public List<Map<String,Object>> getDiskUsage() {
		return jdbcTemplate.queryForList(
			"SELECT id, name, cws_install_type, disk_free_bytes FROM cws_worker");
	}
	
	public List<Map<String,Object>> getLogUsage(String workerId) {
		return jdbcTemplate.queryForList(
			"SELECT filename, size_bytes FROM cws_log_usage WHERE worker_id=?",
			new Object[] { workerId }
			);
	}
	
	public long getDbSize() throws Exception {

		List<Map<String,Object>> list = jdbcTemplate.queryForList(
				"SELECT SUM(data_length + index_length) AS size " +
				"FROM information_schema.TABLES " +
				"WHERE table_schema = (SELECT DATABASE())");
		
		if (list.size() != 1) {
			throw new Exception("Could not get database size.");
		}

		return Long.parseLong(list.get(0).get("size").toString());
	}
	
	
	/**
	 * Returns the number of "up" Workers.
	 * 
	 */
	public int getNumUpWorkers() {
		String query = "SELECT COUNT(*) FROM cws_worker WHERE status = 'up'";
		return jdbcTemplate.queryForObject(query, Integer.class);
	}
	
	
	/**
	 * 
	 */
	public List<Map<String,Object>> getWorkerNumRunningProcs() {
		return jdbcTemplate.queryForList(
			"SELECT cws_worker.id, active_count as cnt " +
			"FROM cws_worker");
	}
	
	
	/**
	 * Gets a list of unresponsive workers.
	 * 
	 */
	public List<Map<String,Object>> detectDeadWorkers(int thresholdMilliseconds) {
		try {
			Timestamp thresholdTimeAgo = new Timestamp(DateTime.now().minusMillis(thresholdMilliseconds).getMillis());
			return jdbcTemplate.queryForList("SELECT * FROM cws_worker " +
						"WHERE last_heartbeat_time < ? AND status = 'up'",
						new Object[] { thresholdTimeAgo });
		}
		catch (Throwable e) {
			cwsEmailerService.sendNotificationEmails("CWS Database Error", "Severe Error!\n\nCould not query database for dead workers.\n\nDetails: " + e.getMessage());
			log.error("Problem occurred while querying the database for dead workers.", e);
			
			throw e;
		}
	}
	
	
	/**
	 * Gets a list of unresponsive external workers.
	 * 
	 */
	public List<Map<String,Object>> detectDeadExternalWorkers(int thresholdMilliseconds) {
		try {
			Timestamp thresholdTimeAgo = new Timestamp(DateTime.now().minusMillis(thresholdMilliseconds).getMillis());
			return jdbcTemplate.queryForList("SELECT * FROM cws_external_worker " +
						"WHERE last_heartbeat_time < ?",
						new Object[] { thresholdTimeAgo });
		}
		catch (Throwable e) {
			cwsEmailerService.sendNotificationEmails("CWS Database Error", "Severe Error!\n\nCould not query database for dead external workers.\n\nDetails: " + e.getMessage());
			log.error("Problem occurred while querying the database for dead external workers.", e);
			
			throw e;
		}
	}
		
	/**
	*
	*/
	public void deleteProcessDefinition(String procDefKey) {
		
		jdbcTemplate.update(
				"DELETE FROM cws_worker_proc_def " +
				"where proc_def_key=?",
				new Object[] {procDefKey});
		
		jdbcTemplate.update(
				"DELETE FROM cws_sched_worker_proc_inst " +
				"where proc_def_key=?",
				new Object[] {procDefKey});
	}
	
	/**
	*
	*/
	public void deleteDeadExternalWorkers(String workerId) {
		jdbcTemplate.update(
			"DELETE FROM cws_external_worker " +
			"where id=?",
			new Object[] {workerId});
	}

	/**
	 * 
	 */
	public int getWorkerJobExecutorMaxPoolSize(String workerId) {
		return jdbcTemplate.queryForObject(
			"SELECT job_executor_max_pool_size FROM cws_worker WHERE id=?",
			new Object[] {workerId}, Integer.class);
	}
	
	
	/**
	 * 
	 */
	public Map<String,Object> getCwsProcessInstanceRowForUuid(String uuid) {
		return jdbcTemplate.queryForMap(
			"SELECT * " +
			"FROM cws_sched_worker_proc_inst " +
			"WHERE uuid=?",
			new Object[] {uuid});
	}

	/**
	 * Updates the job table to set retries, used to retry processes
	 * which have raised an incident and have exhausted their retries
     *
     * If there is an entry in the external task table with no retries,
     * it updates that as well. This allows the process engine to
     * gracefully recover from both failed jobs and failed external
     * task executions.
	 *
	 * Probably can do this in the Camunda API, but this works for now
	 *
	 * @param uuids the proc_inst_ids of the jobs to retry
	 * @param retries the number of retries to give the job
	 * @return number of jobs updated
	 */
	public int setRetriesForUuids(List<String> uuids, int retries) {
		// build list of uuids and sanitize
		String uuidPattern = "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
		String uuidSqlArray = buildSanitizedSqlArray(uuids, uuidPattern);

		// remove the incident rows so they will show as running in the processes page
		// do this first because otherwise a process could raise an incident while retries
		// are still being set, and then the new incident would be deleted
		String query =
			"DELETE FROM ACT_RU_INCIDENT " +
			"WHERE PROC_INST_ID_ IN " + uuidSqlArray;

		jdbcTemplate.update(query);

		String jobQuery =
			"UPDATE ACT_RU_JOB " +
			"SET RETRIES_ = ? " +
			"WHERE RETRIES_ = 0 " +
			"AND PROCESS_INSTANCE_ID_ = ? ";

		// Also set retries for this process's external tasks
		String extTaskQuery =
            "UPDATE ACT_RU_EXT_TASK " +
            "SET RETRIES_ = ? " +
            "WHERE RETRIES_ = 0 " +
            "OR RETRIES_ IS NULL " +
            "AND PROC_INST_ID_ = ? ";

		int updated = 0;
		for(String uuid : uuids) {

			// random sleeps to help mitigate concurrency issues (which tend to be the cause of incidents)
			try {
				Thread.sleep((long)(Math.random() * 50));
			} catch (InterruptedException e) {
				log.error("setRetriesForUuids: Could not sleep thread, proceeding to update anyway. Details: " + e.getMessage());
			}

			// update the job row for this uuid
			updated += jdbcTemplate.update(jobQuery, retries, uuid);

			// update the external task row for this uuid (if it exists)
			updated += jdbcTemplate.update(extTaskQuery, retries, uuid);
		}

		log.trace("updated " + updated + " incident rows, setting retries to " + retries);

		return updated;
	}

	/**
	 * Helper function to sanitize status inputs and build SQL array for use in queries
	 * @param values list of values to put into the array
	 * @param pattern regex to sanitize input
	 * @return SQL-formatted (and sanitized) list of values
	 */
	private String buildSanitizedSqlArray(List<String> values, String pattern) {
		// build array and sanitize input
		StringBuilder sb = new StringBuilder("(");
		if (!values.isEmpty()) {
			int idx = 0;
			for (String i : values) {
				boolean matched = i.matches(pattern);
				if (idx++ == 0 && matched) {
					sb.append("'").append(i).append("'");
				}
				else if (matched) {
					sb.append(",'").append(i).append("'");
				}
			}
		}
		sb.append(")");

		return sb.toString();
	}

	/**
	 * Returns the total number of process instances that
	 * match a set of filters.
	 */
	public int getFilteredProcessInstancesSize(
			String superProcInstId,
			String procInstId,
			String procDefKey,
			String statusList,
			String minDate,
			String maxDate
			)
	{
		List<Object> whereObjs = new ArrayList<Object>();
		if (procInstId != null) { whereObjs.add(procInstId); }
		if (procDefKey != null) { whereObjs.add(procDefKey); }
		if (minDate    != null) { whereObjs.add(minDate);    }
		if (maxDate    != null) { whereObjs.add(maxDate);    }

		String pattern = PENDING + "|" + DISABLED + "|" + FAILED_TO_START + "|" + FAILED_TO_SCHEDULE + "|"
				+ CLAIMED_BY_WORKER + "|" + RUNNING + "|" + COMPLETE + "|" + RESOLVED + "|" + FAIL + "|" + INCIDENT;

		String statusClause = "";
		if (statusList != null) {
			List<String> statuses = Arrays.asList(statusList.split(","));
			statusClause = buildSanitizedSqlArray(statuses, pattern);
		}
		
		log.trace("statusClause = " + statusClause);
		
		int cwsRowsCount =
			jdbcTemplate.queryForObject(
				"SELECT COUNT(*) " +
				"FROM cws_sched_worker_proc_inst " +
				"WHERE " +
				(procInstId != null ? "proc_inst_id=? AND " : "") +
				(procDefKey != null ? "proc_def_key=? AND " : "") +
				(minDate    != null ? "created_time >= ? AND " : "") +
				(maxDate    != null ? "created_time <= ? AND " : "") +
				(statusList != null ? "status IN "+statusClause+" AND " : "") +
				"proc_inst_id IS NULL ", // don't get any started processes
				whereObjs.toArray(), Integer.class);

		// Now add superProcInstId to whereObjs and put at the beginning for SQL query.  Only add if contains "real" procInstId
		if (superProcInstId != null && !superProcInstId.equalsIgnoreCase("NULL")) { whereObjs.add(0, superProcInstId); }

		String camundaCountQuery =
				"SELECT COUNT(*) " +
				"FROM cws_proc_inst_status " +
				"WHERE " +
					(superProcInstId != null ? superProcInstId.equalsIgnoreCase("NULL") ? "super_proc_inst_id IS NULL AND " : "super_proc_inst_id=? AND " : "") +
					(procInstId != null ? "proc_inst_id=? AND " : "") +
					(procDefKey != null ? "proc_def_key=? AND " : "") +
					(statusList != null ? "status IN " + statusClause + " AND " : "") +
					(minDate    != null ? "start_time >= ? AND " : "") +
					(maxDate    != null ? "start_time <= ? AND " : "") +
				"  1=1 ";

		int camundaRowsCount = jdbcTemplate.queryForObject(camundaCountQuery,	whereObjs.toArray(), Integer.class);
		
		log.trace("cwsRowsCount = " + cwsRowsCount + ", camundaRowsCount = " + camundaRowsCount);
		
		return cwsRowsCount + camundaRowsCount;
	}
	
	
	/**
	 * Returns the set of filtered process instances.
	 * 
	 */
	public List<Map<String,Object>> getFilteredProcessInstances(
			String superProcInstId,
			String procInstId,
			String procDefKey,
			String statusList,
			String minDate,
			String maxDate,
			String dateOrderBy,
			int page
			)
	{	
		List<Object> whereObjs = new ArrayList<Object>();
		if (procInstId != null) { whereObjs.add(procInstId); }
		if (procDefKey != null) { whereObjs.add(procDefKey); }
		if (minDate    != null) { whereObjs.add(minDate);    }
		if (maxDate    != null) { whereObjs.add(maxDate);    }
		
		Integer offset = page*PROCESSES_PAGE_SIZE;
		Integer size = PROCESSES_PAGE_SIZE;
		
		whereObjs.add(offset);
		whereObjs.add(size);

		String pattern = PENDING + "|" + DISABLED + "|" + FAILED_TO_START + "|" + FAILED_TO_SCHEDULE + "|"
				+ CLAIMED_BY_WORKER + "|" + RUNNING + "|" + COMPLETE + "|" + RESOLVED + "|" + FAIL + "|" + INCIDENT;

		String statusClause = "";
		if (statusList != null) {
			List<String> statuses = Arrays.asList(statusList.split(","));
			statusClause = buildSanitizedSqlArray(statuses, pattern);
		}

		log.debug("statusClause = " + statusClause);

		String cwsQuery =
				"SELECT * " +
				"FROM cws_sched_worker_proc_inst " +
				"WHERE " +
					(procInstId != null ? "proc_inst_id=? AND " : "") +
					(procDefKey != null ? "proc_def_key=? AND " : "") +
					(minDate    != null ? "created_time >= ? AND " : "") +
					(maxDate    != null ? "created_time <= ? AND " : "") +
					(statusList != null ? "status IN "+statusClause+" AND " : "") +
					"  proc_inst_id IS NULL " + // don't get any started processes
				"ORDER BY created_time " + dateOrderBy + " " +
				"LIMIT ?,?";

		List<Map<String,Object>> cwsRows = jdbcTemplate.queryForList(cwsQuery, whereObjs.toArray());

		// Now add superProcInstId to whereObjs and put at the beginning for SQL query.  Only add if contains "real" procInstId
		if (superProcInstId != null && !superProcInstId.equalsIgnoreCase("NULL")) { whereObjs.add(0, superProcInstId); }

		String camundaQuery =
				"SELECT " +
				// If there is no corresponding row in the CWS, table, then this wasn't scheduled
				"  CI.initiation_key     			AS initiation_key, " +
				"  CI.created_time       			AS created_time, " +
				"  CI.updated_time       			AS updated_time, " +
				"  CI.claimed_by_worker  			AS claimed_by_worker, " +
				"  CI.started_by_worker  			AS started_by_worker, " +
				"  PI.proc_inst_id      			AS proc_inst_id, " +
				"  PI.super_proc_inst_id			AS super_proc_inst_id, " +
				"  PI.proc_def_key      			AS proc_def_key, " +
				"  PI.start_time        			AS proc_start_time, " +
				"  PI.end_time          			AS proc_end_time, " +
				"  PI.status 						AS status " +
				"FROM cws_proc_inst_status PI " +
				"LEFT JOIN cws_sched_worker_proc_inst CI " +
				"ON PI.proc_inst_id = CI.proc_inst_id " +
				"WHERE " +
				(superProcInstId != null ? superProcInstId.equalsIgnoreCase("NULL") ? "PI.super_proc_inst_id IS NULL AND " : "PI.super_proc_inst_id=? AND " : "") +
				(procInstId != null ? "PI.proc_inst_id=? AND " : "") +
				(procDefKey != null ? "PI.proc_def_key=? AND " : "") +
				(statusList != null ? "PI.status IN "+statusClause+" AND " : "") +
				(minDate    != null ? "PI.start_time >= ? AND " : "") +
				(maxDate    != null ? "PI.start_time <= ? AND " : "") +
				" 1=1 " +
				"ORDER BY PI.start_time " + dateOrderBy + " " +
				"LIMIT ?,?";

		List<Map<String,Object>> camundaRows = jdbcTemplate.queryForList(camundaQuery, whereObjs.toArray());
		
		// JOIN THE SETS...
		//
		//  FINAL SET = CWS (PENDING, FAILED_TO_START) + Camunda (running, failed, completed)
		//  
		List<Map<String,Object>> ret = new ArrayList<Map<String,Object>>();
		
		// Get the CWS rows, and add them in.
		//  (these will only be the 'pending' rows
		//
		for (Map<String,Object> cwsRow : cwsRows) {
			Map<String,Object> finalRow = new HashMap<String,Object>();
			finalRow.put("uuid",              (String)cwsRow.get("uuid"));
			finalRow.put("proc_def_key",      (String)cwsRow.get("proc_def_key"));
			finalRow.put("proc_inst_id",      (String)cwsRow.get("proc_inst_id"));
			finalRow.put("super_proc_inst_id", null); // cws rows will never have a super proc inst id since it's started by the user
			finalRow.put("status",            (String)cwsRow.get("status"));
			finalRow.put("initiation_key",    (String)cwsRow.get("initiation_key"));
			finalRow.put("created_time",      (Timestamp)cwsRow.get("created_time"));
			finalRow.put("updated_time",      (Timestamp)cwsRow.get("updated_time"));
			finalRow.put("claimed_by_worker", (String)cwsRow.get("claimed_by_worker"));
			finalRow.put("started_by_worker", (String)cwsRow.get("started_by_worker"));
			finalRow.put("proc_start_time",   null); // pending rows haven't actually run yet
			finalRow.put("proc_end_time",     null); // pending rows haven't actually run yet
			ret.add(finalRow);
		}
		
		// Get the Camunda rows, and add them in
		//
		for (Map<String,Object> camundaRow : camundaRows) {
			Map<String,Object> finalRow = new HashMap<String,Object>();
			finalRow.put("uuid",              "TODO");
			finalRow.put("proc_def_key",      (String)camundaRow.get("proc_def_key"));
			finalRow.put("proc_inst_id",      (String)camundaRow.get("proc_inst_id"));
			finalRow.put("super_proc_inst_id", (String)camundaRow.get("super_proc_inst_id"));
			if (finalRow.get("status") == null || !finalRow.get("status").equals(FAILED_TO_START)) {
				finalRow.put("status",            (String)camundaRow.get("status"));
			}
			finalRow.put("initiation_key",    (String)camundaRow.get("initiation_key"));
			finalRow.put("created_time",      (Timestamp)camundaRow.get("created_time"));
			finalRow.put("updated_time",      (Timestamp)camundaRow.get("updated_time"));
			finalRow.put("claimed_by_worker", (String)camundaRow.get("claimed_by_worker"));
			finalRow.put("started_by_worker", (String)camundaRow.get("started_by_worker"));
			finalRow.put("proc_start_time",   (Timestamp)camundaRow.get("proc_start_time"));
			finalRow.put("proc_end_time",     (Timestamp)camundaRow.get("proc_end_time"));
			ret.add(finalRow);
		}
		
		return ret;
	}


	/**
	 * Used on Deployment page to get process instance statistics.
	 *
	 * @param lastNumHours how many hours back to query for
	 * @return List of proc_def_key, status, count
	 */
	public List<Map<String,Object>> getProcessInstanceStats(String lastNumHours) {
		List<Map<String,Object>> ret = new ArrayList<>();

		Timestamp time = new Timestamp(0L);

		if (lastNumHours != null) {
			time = new Timestamp(DateTime.now().minusHours(Integer.parseInt(lastNumHours)).getMillis());
		}

		String query =
				"SELECT  " +
				"	proc_def_key,  " +
				"	status,  " +
				"   COUNT(*) AS cnt  " +
				"FROM cws_sched_worker_proc_inst " +
				"WHERE status IN ('" + PENDING + "', '" + DISABLED + "', '" + FAILED_TO_START + "') " +
				"AND (created_time > ? OR updated_time > ?) " +
				"GROUP BY proc_def_key, status " +
				"UNION ALL " +
				"SELECT " +
				"	proc_def_key, " +
				"   status, " +
				"   COUNT(*) AS cnt " +
				"FROM cws_proc_inst_status " +
				"WHERE (start_time > ? OR end_time > ?) " +
				"GROUP BY proc_def_key, status ";

		List<Map<String,Object>> camundaAndCwsStatuses = jdbcTemplate.queryForList(query, time, time, time, time);

		ret.addAll(camundaAndCwsStatuses);

		return ret;
	}

	/**
	 * Get statistics for a single proc_def_key, business_key pair
	 *
	 * @param procDefKey The process definition key of the model
	 * @param businessKey The business key of the instance
	 * @return List of status, count
	 */
	public List<Map<String,Object>> getStatusByBusinessKey(String procDefKey, String businessKey) {

		String query =
				"SELECT  " +
				"	status,  " +
				"   COUNT(*) AS cnt  " +
				"FROM cws_sched_worker_proc_inst " +
				"WHERE status IN ('" + PENDING + "', '" + DISABLED + "', '" + FAILED_TO_START + "') " +
				"AND proc_business_key = ? " +
				"AND proc_def_key = ? " +
				"GROUP BY proc_def_key, status " +
				"UNION ALL " +
				"SELECT " +
				"   status, " +
				"   COUNT(*) AS cnt " +
				"FROM cws_proc_inst_status " +
				"WHERE business_key = ? " +
				"AND proc_def_key = ? " +
				"GROUP BY proc_def_key, status ";

		List<Map<String,Object>> camundaAndCwsStatuses = jdbcTemplate.queryForList(query, businessKey, procDefKey, businessKey, procDefKey);

		return new ArrayList<>(camundaAndCwsStatuses);
	}
	
	/**
	 * Used by worker syncCounters method
	 * 
	 * List of [uuid, proc_def_key, status]
	 */
	public List<Map<String,Object>> getStatsForScheduledProcs(Set<String> cwsSchedUuids) {
		List<Map<String,Object>> ret = new ArrayList<Map<String,Object>>();
		if (!cwsSchedUuids.isEmpty()) {
			String uuidInClause = "(";
			int idx = 0;
			for (String uuid : cwsSchedUuids) {
				if (idx++ == 0) { uuidInClause+="'"+uuid+"'"; }
				else { uuidInClause+=",'"+uuid+"'"; }
			}
			uuidInClause += ")";
			//log.debug(uuidInClause);
			
			List<Map<String,Object>> camundaStatuses = jdbcTemplate.queryForList(
							"SELECT DISTINCT " +
							"  CI.uuid AS uuid, " +
							"  PI.PROC_DEF_KEY_ AS proc_def_key, " +
								PROC_INST_STATUS_SQL + " AS status " +
							"FROM ACT_HI_PROCINST PI " +
							"  LEFT JOIN cws_sched_worker_proc_inst CI " +
							"    ON PI.PROC_INST_ID_ = CI.proc_inst_id " +
							"  LEFT JOIN ACT_HI_ACTINST AI " +
							"    ON " +
							"    PI.PROC_INST_ID_ = AI.PROC_INST_ID_ " +
							"    AND " +
							"    (AI.END_TIME_ is null or AI.ACT_TYPE_ LIKE '%ndEvent' AND PI.END_TIME_ IS NOT NULL) " + 
							"WHERE " +
							"  (AI.PARENT_ACT_INST_ID_ IS NULL OR AI.PARENT_ACT_INST_ID_ NOT LIKE 'SubProcess%') " +
							"  AND " +
							PROC_INST_STATUS_SQL + " IN ('complete','running', 'fail') " +
							"  AND " +
							"  CI.uuid in " + uuidInClause);
			
			List<Map<String,Object>> cwsStatuses = jdbcTemplate.queryForList(
					"SELECT DISTINCT " +
							"  uuid, " +
							"  proc_def_key, " +
							"  status " +
							"FROM cws_sched_worker_proc_inst " +
							"WHERE " +
							"  uuid in " + uuidInClause + " " +
							"  AND " +
							"  proc_inst_id IS NULL "  // don't get any started processes (covered in above query)
					);
			
			log.debug(camundaStatuses.size() + " camunda rows,  " + cwsStatuses.size() + " cwsStatuses rows.");
			
			ret.addAll(camundaStatuses);
			ret.addAll(cwsStatuses);
		}
		
		return ret;
	}
	
	
	/**
	 * 
	 */
	public List<Map<String,Object>> getRunningProcessInstances() {
		return jdbcTemplate.queryForList(
			"SELECT proc_inst_id, uuid, proc_def_key, status " +
			"FROM cws_sched_worker_proc_inst " +
			"WHERE status='running'");
	}
	
	
	/**
	 * 
	 */
	public List<Map<String,Object>> getIncompleteProcessInstancesForWorker(String workerId) {
		return jdbcTemplate.queryForList(
			"SELECT proc_def_key, COUNT(*) as cnt " +
			"FROM cws_sched_worker_proc_inst " +
			"WHERE started_by_worker=? AND status IN ('pending', 'running') " +
			"GROUP BY proc_def_key",
			new Object[] {workerId});
	}
	
	
	/**
	 * 
	 */
	public List<Map<String,Object>> getPendingProcessInstances() {
		return jdbcTemplate.queryForList(
			"SELECT * " +
			"FROM cws_sched_worker_proc_inst " +
			"WHERE status = '" + PENDING + "'");
	}
	
	
	/**
	 * Returns mapping of process instance statuses.
	 * If procInstId is specified, then returns information about a single process instance,
	 * otherwise information about all process instances is returned.
	 * 
	 * Camunda will first create a row in ACT_HI_PROCINST upon process start.
	 * Then when the process completes, it will create multiple rows (one per activity) in ACT_HI_ACTINST.
	 * 
	 */
	public List<Map<String,Object>> getProcInstStatus(String procInstId, Integer limit) {
		String query =
			"SELECT DISTINCT " +
			"  PI.PROC_DEF_ID_ AS procDefKey," +
			"  PI.PROC_INST_ID_ AS procInstId, " +
			"  PI.START_TIME_ AS startTime, " +
			"  PI.END_TIME_ AS endTime, " +
			"  PI.DURATION_ AS duration, " +
			"  PI.END_ACT_ID_ AS endActivityId, " +
				PROC_INST_STATUS_SQL + " AS status " +
			"FROM " +
			"  ACT_HI_PROCINST PI " +
			"  LEFT JOIN " +
			"  ACT_HI_ACTINST AI " +
			" ON " +
			"  PI.PROC_INST_ID_ = AI.PROC_INST_ID_ AND " +
			"  ((PI.END_ACT_ID_ IS NULL) OR AI.ACT_ID_ = PI.END_ACT_ID_) " +
			"WHERE " +
			"  PI.PROC_INST_ID_ = '"+procInstId+"' AND " +
			// don't be fooled by sub-process end events.
			// This clause was put in because of CWS-350
			"  AI.PARENT_ACT_INST_ID_ NOT LIKE 'SubProcess%' " +
			"ORDER BY PI.START_TIME_ desc " +
			(limit == null ? "" : " LIMIT "+limit);
		log.trace("QUERY: " + query);
		return jdbcTemplate.queryForList(query);
	}
	
	
	/**
	 * 
	 */
	public void updateWorkerProcDefEnabled(String workerId, String procDefKey, String deploymentId, boolean isEnabled) throws Exception {
		int numUpdated = 0;
		if (isEnabled) {
			// If we are enabling a proc def, then first try inserting a new row.
			// If the insert actually inserted a new row, then we are done.
			// If the row already existed, then update the row.
			//
			numUpdated = jdbcTemplate.update(
				"INSERT IGNORE INTO cws_worker_proc_def " +
				"(worker_id, proc_def_key, max_instances, deployment_id, accepting_new) " +
				"VALUES (?, ?, ?, ?, ?)",
				new Object[] {workerId, procDefKey, DEFAULT_WORKER_PROC_DEF_MAX_INSTANCES, deploymentId, isEnabled});
			if (numUpdated == 0) { // row was already there, so just update it
				numUpdated = jdbcTemplate.update(
					"UPDATE cws_worker_proc_def " +
					"SET accepting_new=1 " +
					"WHERE worker_id=? AND proc_def_key=?",
					new Object[] {workerId, procDefKey});
				log.debug("Updated (set accepting_new = 1) " + numUpdated + " row(s) in the cws_worker_proc_def table...");
			}
			else {
				log.debug("Inserted " + numUpdated + " row(s) into the cws_worker_proc_def table...");
			}
		}
		else {
			// If we are disabling a process definition,
			// then update the accepting_new flag
			//
			numUpdated = jdbcTemplate.update(
				"UPDATE cws_worker_proc_def " +
				"SET accepting_new=0 " +
				"WHERE worker_id=? AND proc_def_key=?",
				new Object[] {workerId, procDefKey});
			log.debug("Updated (set accepting_new = 0) " + numUpdated + " row(s) in the cws_worker_proc_def table...");
		}
	}
	
	
	/**
	 * 
	 */
	public void updateWorkerProcDefLimit(String workerId, String procDefKey, int newLimit) throws Exception {
		int numUpdated = jdbcTemplate.update(
			"UPDATE cws_worker_proc_def " +
			"SET max_instances=? " +
			"WHERE worker_id=? AND proc_def_key=?",
			new Object[] {newLimit, workerId, procDefKey});
		
		log.debug("updated "+numUpdated + " rows");
	}
	
	
	/**
	 * 
	 */
	public void updateWorkerProcDefDeploymentId(String workerId, String procDefKey, String newDeploymentId) throws Exception {
		int numUpdated = jdbcTemplate.update(
			"UPDATE cws_worker_proc_def " +
			"SET deployment_id=? " +
			"WHERE worker_id=? AND proc_def_key=?",
			new Object[] {newDeploymentId, workerId, procDefKey});
		
		log.trace("updated "+numUpdated + " rows");
	}
	
	
	/**
	 * 
	 */
	public void updateWorkerNumJobExecutorThreads(String workerId, int numThreads) {
		int numUpdated = jdbcTemplate.update(
			"UPDATE cws_worker " +
			"SET job_executor_max_pool_size=? " +
			"WHERE id=?",
			new Object[] {numThreads, workerId});

		log.debug("updated "+numUpdated + " rows");
	}
	
	
	/**
	 * 
	 */
	public void updateWorkerStatus(String workerId, String status) {
		int numRowsUpdated = jdbcTemplate.update(
			"UPDATE cws_worker SET status=? WHERE id=?",
			new Object[] { status, workerId }
		);
		log.debug("setWorkerStatus ('" + workerId + "', " + status + "): Updated " + numRowsUpdated + " rows.");
	}
	
	public void setWorkerAcceptingNew(boolean acceptingNew, String workerId) {
		jdbcTemplate.update(
			"UPDATE cws_worker_proc_def " +
			"SET accepting_new=? " +
			"WHERE worker_id=?",
			new Object[] { acceptingNew, workerId }
		);
	}
	
	
	/**
	 * 
	 */
	public List<Map<String,Object>> getWorkerProcDefRows() {
		return jdbcTemplate.queryForList(
			"SELECT * FROM cws_worker_proc_def");
	}
	
	
	/**
	 * Get proc defs where accepting_new is 1, and worker is alive (up)
	 */
	public List<String> getAcceptableProcDefKeys() {
		return jdbcTemplate.queryForList(
			"SELECT DISTINCT proc_def_key " +
			"FROM cws_worker_proc_def WPD JOIN cws_worker W " +
			"ON W.status='up' AND W.id=WPD.worker_id AND WPD.accepting_new = 1",
			String.class
		);
	}
	
	
	/**
	*
	*/
	public List<Map<String,Object>> getWorkersForProcDefKey(String procDefKey){
		return jdbcTemplate.queryForList(
			"SELECT w.id, w.name, w.status, w.cws_install_type, pd.proc_def_key,pd.accepting_new "+
			"FROM cws_worker AS w "+
			"LEFT JOIN cws_worker_proc_def AS pd ON w.id=pd.worker_id AND "+
			"(pd.proc_def_key='" + procDefKey + "' OR pd.proc_def_key IS NULL) ORDER BY w.name");
	}
	
	
	/**
	*
	*/
	public List<Map<String,Object>> getProcDefWorkerCount(){
		return jdbcTemplate.queryForList(
			"SELECT prc.KEY_ AS pdk , IFNULL(SUM(pd.accepting_new),0) AS workers "+
			"FROM ACT_RE_PROCDEF AS prc "+
			"LEFT JOIN cws_worker_proc_def AS pd ON prc.KEY_=pd.proc_def_key GROUP BY KEY_");
		
		// Simpler? :
		// select proc_def_key as pdk, sum(accepting_new) as workers from cws_worker_proc_def group by proc_def_key;
	}
	
	
	/**
	 * 
	 */
	public Integer isWorkerProcDefAcceptingNew(String workerId, String deploymentId) {
		try {
			return jdbcTemplate.queryForObject(
				"SELECT accepting_new " +
				"FROM cws_worker_proc_def " +
				"WHERE worker_id=? AND deployment_id=?",
				new Object[]{workerId, deploymentId}, Integer.class);
		}
		catch (EmptyResultDataAccessException e) {
			log.debug("no cws_worker_proc_def row found for workerId = '" + workerId + "', deploymentId = '" + deploymentId + "'");
			return null;
		}
	}
	
	
	/**
	 * 
	 */
	public List<Map<String,Object>> getWorkerProcDefRows(String workerId, Boolean acceptingNew) {
		if (acceptingNew != null) {
			return jdbcTemplate.queryForList(
				"SELECT * FROM cws_worker_proc_def " +
				"WHERE worker_id=? AND accepting_new=?",
				new Object[]{workerId, acceptingNew});
		}
		else {
			return jdbcTemplate.queryForList(
				"SELECT * FROM cws_worker_proc_def " +
				"WHERE worker_id=?",
				new Object[]{workerId});
		}
	}
	
	
	/**
	*
	*/
	public void insertCwsToken(String cwsToken, String username, Timestamp expirationTime) {
		jdbcTemplate.update(
			"INSERT IGNORE INTO cws_token " +
			"(token, username, expiration_time) " +
			"VALUES (?, ?, ?)",
			new Object[] {cwsToken, username, expirationTime});
	}
	
	
	/**
	*
	*/
	public void deleteCwsToken(String cwsToken, String username) {
		jdbcTemplate.update(
			"DELETE FROM cws_token " +
			"where token=? and username=?",
			new Object[] {cwsToken, username});
	}
	
	
	/**
	*
	*/
	public Map<String,Object> getCwsToken(String cwsToken, String username) {
		return jdbcTemplate.queryForMap(
			"SELECT * FROM cws_token " +
			"WHERE token=? AND username=?",
			new Object[] { cwsToken, username }
		);
	}
	
	
	/**
	*
	*/
	public List<Map<String,Object>> getAllCwsTokens() {
		return jdbcTemplate.queryForList(
			"SELECT * FROM cws_token"
		);
	}

	/**
	 * Update status
	 * @param status New status
	 * @param procInstId Process Instance ID
	 * @return Number of rows affected
	 */
	private int updateStatus(String status, String procInstId) {
		String query =
			"UPDATE cws_proc_inst_status " +
			"SET status = ? " +
			"WHERE proc_inst_id = ? ";

		return jdbcTemplate.update(query, status, procInstId);
	}

	/**
	 * Update status
	 * @param status New status
	 * @param procInstIds Process Instance IDs
	 * @return Number of rows affected
	 */
	private int updateStatusBulk(String status, List<String> procInstIds) {
		// build list of uuids and sanitize
		String uuidPattern = "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
		String uuidSqlArray = buildSanitizedSqlArray(procInstIds, uuidPattern);

		String query =
			"UPDATE cws_proc_inst_status " +
			"SET status = ? " +
			"WHERE proc_inst_id IN " + uuidSqlArray;

		return jdbcTemplate.update(query, status);
	}

	/**
	 * Change status to resolved
	 * @param procInstId Process Instance ID
	 * @return Number of rows affected
	 */
	public int makeResolved(String procInstId) {
		return updateStatus("resolved", procInstId);
	}

	/**
	 * Change status to resolved for multiple proc_inst_id
	 * @param procInstIds Process Instance ID
	 * @return Number of rows affected
	 */
	public int makeResolvedBulk(List<String> procInstIds) {
		return updateStatusBulk("resolved", procInstIds);
	}

	/**
	 * Retry failedToStart
	 * @param uuids CWS uuids
	 * @return Number of rows affected
	 */
	public int retryFailedToStart(List<String> uuids) {
		// build list of uuids and sanitize
		String uuidPattern = "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
		String uuidSqlArray = buildSanitizedSqlArray(uuids, uuidPattern);

		String query =
			"UPDATE cws_sched_worker_proc_inst " +
			"SET status = 'pending' " +
			"WHERE uuid IN " + uuidSqlArray;

		return jdbcTemplate.update(query);
	}
}
