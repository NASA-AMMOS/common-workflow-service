package jpl.cws.core.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Helper / service methods related to the database.
 * 
 * @author ghollins
 *
 */
public class DbService {
	private static final Logger log = LoggerFactory.getLogger(DbService.class);

	@Autowired protected JdbcTemplate jdbcTemplate;
	@Autowired protected NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired protected JdbcTemplate jdbcAdaptationTemplate;

	protected static final long SLOW_WARN_THRESHOLD = 100;
	protected static final int IN_CLAUSE_MAX_ELEMENTS = 100;
	
	public DbService() {
		log.trace("DbService constructor...");
	}
	
	/**
	 * Returns a list of all CWS table names.
	 */
	public List<String> getCwsTableNames() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		ResultSet tables = conn.getMetaData().getTables(null, null, "cws%", null);
		List<String> cwsTableNames = new ArrayList<String>();
		while (tables.next()) {
			String tableName = tables.getString("TABLE_NAME").toString();
			log.trace("TABLES NAME: "+tableName);
			cwsTableNames.add(tableName);
		}
		tables.close(); // is this line really necessary, or will below call take care of this?
		conn.close();
		return cwsTableNames;
	}
	
	
	/**
	 * This method is shared by both cws-service and cws-engine
	 */
	public void createIfNotExistsNodeProcess(
			String workerId, 
			String procDefKey,
			String deploymentId,
			int maxInstances) {
		log.trace("Inserting into cws_worker_proc_def table...");
		int numRowsAffected = jdbcTemplate.update(
			"INSERT IGNORE INTO cws_worker_proc_def " +
			"(worker_id, proc_def_key, max_instances, deployment_id, accepting_new) " +
			"VALUES (?,?,?,?,?) ", 
			new Object[] {workerId, procDefKey, maxInstances, deploymentId, true});
		if (numRowsAffected >= 1) {
			log.info("Inserted " + numRowsAffected + " rows into cws_worker_proc_def table.");
		}
	}
	
	
	/**
	 * This method may be called by multiple web applications.
	 * The first web application to call this will create tables if necessary.
	 * The other web applications will effectively skip over this code.
	 * 
	 */
	public void initCwsTables() throws SQLException {
		log.info("START Initializing CWS tables...");
		log.info("DONE Initializing CWS tables.");
	}
	
	
	/**
	 * Changes the status of cws_sched_worker_proc_inst row(s) in the database.
	 * 
	 */
	public int changeSchedWorkerProcInstRowStatus(
			String oldStatus,
			String newStatus,
			List<String> limitToUuids) throws Exception {
		log.trace("changeSchedWorkerProcInstRowStatus...");
		
		int numRowsAffected = 0;
		
		if (limitToUuids != null && !limitToUuids.isEmpty()) {
			if (limitToUuids.size() > IN_CLAUSE_MAX_ELEMENTS) {
				throw new Exception("Number of elements to change ("+limitToUuids.size()+
						") exceeds limit for IN clause ("+IN_CLAUSE_MAX_ELEMENTS+"). " +
						"Please select a smaller set.");
			}
			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("uuids", limitToUuids);
			parameters.addValue("oldStatus", oldStatus);
			parameters.addValue("newStatus", newStatus);
			
			numRowsAffected = namedJdbcTemplate.update(
			"UPDATE cws_sched_worker_proc_inst " +
			"SET status = :newStatus WHERE status = :oldStatus AND " +
			"uuid IN (:uuids)",
			parameters);
		}
		else {
			numRowsAffected = jdbcTemplate.update(
				"UPDATE cws_sched_worker_proc_inst " +
				"SET status = ? WHERE status = ?",
				new Object[] {newStatus, oldStatus});
		}
		
		if (numRowsAffected >= 1) {
			log.info("Transitioned " + numRowsAffected + " rows in cws_sched_worker_proc_inst table from 'pending' to 'disabled'.");
		}
		
		return numRowsAffected;
	}

}
