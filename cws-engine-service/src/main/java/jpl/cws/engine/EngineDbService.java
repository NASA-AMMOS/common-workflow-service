package jpl.cws.engine;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;

import jpl.cws.core.db.DbService;
import jpl.cws.core.log.CwsWorkerLoggerFactory;

/**
 * Helper / service methods related to the database.
 * 
 * @author ghollins
 *
 */
public class EngineDbService extends DbService implements InitializingBean {
	@Autowired private CwsWorkerLoggerFactory cwsWorkerLoggerFactory;
	
	@Value("${cws.worker.id}") private String workerId;
	@Value("${cws.install.type}") private String cwsInstallType;
	@Value("${cws.worker.type}") private String cwsWorkerType;
	@Value("${camunda.executor.service.max.pool.size}") private int maxExecutorServicePoolSize;
	
	private Logger log;
	
	public EngineDbService() {
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log = cwsWorkerLoggerFactory.getLogger(this.getClass());
		log.trace("EngineDbService afterPropertiesSet...");
		log.trace("jdbcTemplate = " + jdbcTemplate);
		log.trace("workerId     = " + workerId);
	}
	
	public boolean workerExists() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM cws_worker WHERE id=?", new Object[]{workerId}, Integer.class) > 0;
	}
	
	private boolean workerLogExists(String filename) {
		return jdbcTemplate.queryForObject(
			"SELECT count(*) FROM cws_log_usage WHERE worker_id=? AND filename=?", new Object[]{workerId, filename}, Integer.class) > 0;
	}

	public int numWorkers() {
		return jdbcTemplate.queryForObject(
			"SELECT count(*) FROM cws_worker", Integer.class);
	}
	
	private void insertWorkerLog(String filename, long size) {
		
		jdbcTemplate.update(
				"INSERT INTO cws_log_usage (worker_id, filename, size_bytes) " +
				"VALUES (?,?,?)",
				new Object[] { workerId, filename, size }
			);
	}
	
	private void updateWorkerLog(String filename, long size) {
	
		jdbcTemplate.update(
				"UPDATE cws_log_usage SET size_bytes=? WHERE worker_id=? AND filename=?", 
				new Object[] {size, workerId, filename }
				);
	}

	private void deleteWorkerLog(String filename) {
		jdbcTemplate.update(
				"DELETE FROM cws_log_usage WHERE worker_id=? AND filename=?", 
				new Object[] { workerId, filename }
				);
	}
	
	private void updateWorkerDiskFreeBytes(long diskFreeBytes) {
	
		jdbcTemplate.update(
				"UPDATE cws_worker SET disk_free_bytes=? WHERE id=?", 
				new Object[] { diskFreeBytes, workerId }
				);
	}
	
	private List<Map<String,Object>> getWorkerLogs() {
		return jdbcTemplate.queryForList(
			"SELECT filename FROM cws_log_usage WHERE worker_id=?",
			new Object[] { workerId });
	}
	
	private boolean findFilename(File[] files, String name) {
	
		for (File file : files) {
			
			if (file.getName().equals(name)) {
				
				return true;
			}
		}
		
		return false;
	}
	
	public void updateStats(File[] logs, long diskFreeBytes) {

		updateWorkerDiskFreeBytes(diskFreeBytes);
		
		// Update or insert into database for found log files
		for (File log : logs) {

			if (workerLogExists(log.getName())) {
				
				updateWorkerLog(log.getName(), log.length());
			}
			else {
				insertWorkerLog(log.getName(), log.length());
			}
		}
		
		// Delete logs from database that are no longer found
		List<Map<String,Object>> rows = getWorkerLogs();
		
		for (Map<String, Object> row : rows) {
		
			String filename = row.get("filename").toString();
			
			if (!findFilename(logs, filename)) {
				
				// File not found so remove from database
				deleteWorkerLog(filename);
			}
		}
	}
	
	
	/**
	 * Create a row (if not already exists) in the database for this worker
	 */
	public void createOrUpdateWorkerRow(String lockOwner) {
		if (!workerExists()) {
			log.info("Inserting row into cws_worker table...");
			
			int numUpdated = 0;
			int numTries = 0;
			while (numTries++ < 10 && numUpdated != 1) {
				Timestamp tsNow = new Timestamp(DateTime.now().getMillis());
				String workerName = "worker" + String.format("%1$4s", numWorkers()).replace(' ', '0');
				
				try {
					numUpdated = jdbcTemplate.update(
							"INSERT INTO cws_worker" +
							"   (id, lock_owner, name, install_directory, cws_install_type, cws_worker_type, " +
							"    status, job_executor_max_pool_size, created_time, last_heartbeat_time) " +
							"VALUES (?,?,?,?,?,?,?,?,?,?)",
							new Object[] {
									workerId,
									lockOwner,
									workerName,
									"install_directory_TODO",
									cwsInstallType,
									cwsWorkerType,
									null, // status will be changed to null once the worker is fully initialized
									maxExecutorServicePoolSize, // changeable later via the UI..
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
				log.error("Could not create worker row for workerId " + workerId + " !");
			}
		}
		else {
			log.info("Updating row in cws_worker table...");
			
			int numUpdated = 0;
			int numTries = 0;
			while (numTries++ < 10 && numUpdated != 1) {
				Timestamp tsNow = new Timestamp(DateTime.now().getMillis());
				
				try {
					numUpdated = jdbcTemplate.update(
							"UPDATE cws_worker SET lock_owner=?, job_executor_max_pool_size=?, last_heartbeat_time=? WHERE id=?", 
							new Object[] {
									lockOwner,
									maxExecutorServicePoolSize, // changeable later via the UI..
									tsNow,  					// last_heartbeat_time
									workerId
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
				log.error("Could not update worker row for workerId " + workerId + " !");
			}
			
			log.debug("Updated cws_worker row for ID: "+workerId);
		}
	}
	
	
	public void workerHeartbeat(int activeCount) {
		jdbcTemplate.update(
			"UPDATE cws_worker SET last_heartbeat_time = ?, status='up', active_count = ? WHERE id=?",
			new Object[] { new Timestamp(DateTime.now().getMillis()), activeCount, workerId }
		);
	}
	
	
	public int getMaxInstancesForProcDef(String procDefKey) {
		return jdbcTemplate.queryForObject(
			"SELECT max_instances FROM cws_worker_proc_def " +
			"WHERE proc_def_key=? AND worker_id=?",
			new Object[]{procDefKey, workerId}, Integer.class);
	}
	
}
