import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debug utility to populate the CWS database with a configurable number of finished jobs
 * for testing and performance analysis purposes.
 */
public class PopulateFinishedJobs {

    // Configuration constants
    private static final int DEFAULT_JOB_COUNT = 30000;
    private static final String[] PROCESS_DEF_KEYS = {"simple_test_process", "email_task_test", "rest_get_task", "cmd_line_exec_task"};
    private static final String[] JOB_STATUSES = {"complete", "fail", "resolved"};
    private static final String[] WORKER_IDS = {"worker_001", "worker_002", "worker_003", "worker_004"};
    private static final String[] SAMPLE_BUSINESS_KEYS = {"test-key-", "prod-key-", "dev-key-", "qa-key-"};
    private static final int MAX_DAYS_AGO = 180; // Jobs distributed over last 6 months
    
    // Database configuration - will be overridden by command line arguments
    private static String DB_URL = "jdbc:mysql://db:3306/cws";
    private static String DB_USER = "root";
    private static String DB_PASS = "changeme";
    
    // Counters
    private static final AtomicInteger currentJobId = new AtomicInteger(1);
    private static final Random random = new Random();

    public static void main(String[] args) {
        int jobCount = DEFAULT_JOB_COUNT;
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--count":
                    if (i + 1 < args.length) {
                        jobCount = Integer.parseInt(args[i + 1]);
                        i++;
                    }
                    break;
                case "--url":
                    if (i + 1 < args.length) {
                        DB_URL = args[i + 1];
                        i++;
                    }
                    break;
                case "--user":
                    if (i + 1 < args.length) {
                        DB_USER = args[i + 1];
                        i++;
                    }
                    break;
                case "--pass":
                    if (i + 1 < args.length) {
                        DB_PASS = args[i + 1];
                        i++;
                    }
                    break;
                case "--help":
                    printUsage();
                    return;
            }
        }
        
        System.out.println("Starting database population with " + jobCount + " finished jobs...");
        populateFinishedJobs(jobCount);
        System.out.println("Completed populating database with " + jobCount + " finished jobs.");
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -cp .:./lib/* PopulateFinishedJobs [options]");
        System.out.println("Options:");
        System.out.println("  --count <number>    Number of jobs to create (default: 30000)");
        System.out.println("  --url <url>         Database URL (default: jdbc:mysql://db:3306/cws)");
        System.out.println("  --user <username>   Database username (default: admin)");
        System.out.println("  --pass <password>   Database password (default: changeme)");
        System.out.println("  --help              Show this help message");
    }
    
    /**
     * Main method to populate the database with finished jobs
     */
    private static void populateFinishedJobs(int jobCount) {
        Connection conn = null;
        
        try {
            // Connect to the database
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            conn.setAutoCommit(false);
            System.out.println("Connected to database");
            
            // Prepare statements
            PreparedStatement procInstStmt = conn.prepareStatement(
                "INSERT INTO cws_sched_worker_proc_inst " +
                "(uuid, created_time, updated_time, proc_inst_id, proc_def_key, proc_business_key, " +
                "priority, proc_variables, status, error_message, initiation_key, " +
                "claimed_by_worker, started_by_worker, last_rejection_worker, num_worker_attempts, claim_uuid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            PreparedStatement procStatusStmt = conn.prepareStatement(
                "INSERT INTO cws_proc_inst_status " +
                "(proc_inst_id, proc_def_key, super_proc_inst_id, business_key, " +
                "status, start_time, end_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)");
            
            // Batch insert for better performance
            int batchSize = 1000;
            int progress = 0;
            
            System.out.println("Starting batch inserts...");
            
            for (int i = 0; i < jobCount; i++) {
                // Generate job data
                String uuid = UUID.randomUUID().toString();
                String procInstId = UUID.randomUUID().toString();
                String procDefKey = PROCESS_DEF_KEYS[random.nextInt(PROCESS_DEF_KEYS.length)];
                String businessKey = SAMPLE_BUSINESS_KEYS[random.nextInt(SAMPLE_BUSINESS_KEYS.length)] + currentJobId.getAndIncrement();
                int priority = random.nextInt(10) + 1; // Priority between 1-10
                
                // Generate random timestamps in the past
                int daysAgo = random.nextInt(MAX_DAYS_AGO);
                int runningTimeMinutes = random.nextInt(60) + 1; // 1-60 minutes
                
                LocalDateTime createdDateTime = LocalDateTime.now().minusDays(daysAgo);
                LocalDateTime claimedDateTime = createdDateTime.plusMinutes(random.nextInt(10) + 1); // 1-10 minutes later
                LocalDateTime startedDateTime = claimedDateTime.plusMinutes(random.nextInt(5) + 1); // 1-5 minutes later
                LocalDateTime endDateTime = startedDateTime.plusMinutes(runningTimeMinutes);
                
                Timestamp createdTimestamp = Timestamp.valueOf(createdDateTime);
                Timestamp updatedTimestamp = Timestamp.valueOf(endDateTime);
                Timestamp startTimestamp = Timestamp.valueOf(startedDateTime);
                Timestamp endTimestamp = Timestamp.valueOf(endDateTime);
                
                // Pick worker, status
                String workerId = WORKER_IDS[random.nextInt(WORKER_IDS.length)];
                String status = JOB_STATUSES[random.nextInt(JOB_STATUSES.length)];
                
                // Create variables map
                Map<String, String> procVariables = new HashMap<>();
                procVariables.put("uuid", uuid);
                procVariables.put("procDefKey", procDefKey);
                procVariables.put("procBusinessKey", businessKey);
                procVariables.put("procPriority", String.valueOf(priority));
                procVariables.put("workerId", workerId);
                procVariables.put("initiationKey", "debug_population");
                procVariables.put("param1", "value" + i);
                procVariables.put("param2", "testing" + i);
                
                // Convert variables to byte array (simplified)
                byte[] variablesBytes = serializeVariables(procVariables);
                
                // Insert into cws_sched_worker_proc_inst
                procInstStmt.setString(1, uuid);
                procInstStmt.setTimestamp(2, createdTimestamp);
                procInstStmt.setTimestamp(3, updatedTimestamp);
                procInstStmt.setString(4, procInstId);
                procInstStmt.setString(5, procDefKey);
                procInstStmt.setString(6, businessKey);
                procInstStmt.setInt(7, priority);
                procInstStmt.setBytes(8, variablesBytes);
                procInstStmt.setString(9, status);
                procInstStmt.setString(10, status.equals("fail") ? "Sample error message for debugging" : null);
                procInstStmt.setString(11, "debug_population");
                procInstStmt.setString(12, workerId);
                procInstStmt.setString(13, workerId);
                procInstStmt.setString(14, null);
                procInstStmt.setInt(15, 1);
                procInstStmt.setString(16, UUID.randomUUID().toString());
                procInstStmt.addBatch();
                
                // Insert into cws_proc_inst_status
                procStatusStmt.setString(1, procInstId);
                procStatusStmt.setString(2, procDefKey);
                procStatusStmt.setString(3, null); // No super process instance
                procStatusStmt.setString(4, businessKey);
                procStatusStmt.setString(5, status);
                procStatusStmt.setTimestamp(6, startTimestamp);
                procStatusStmt.setTimestamp(7, endTimestamp);
                procStatusStmt.addBatch();
                
                // Execute batch if needed
                if ((i + 1) % batchSize == 0) {
                    procInstStmt.executeBatch();
                    procStatusStmt.executeBatch();
                    conn.commit();
                    
                    // Update and print progress
                    progress = (i + 1) * 100 / jobCount;
                    System.out.println("Progress: " + progress + "% (" + (i + 1) + "/" + jobCount + ")");
                }
            }
            
            // Execute final batch if any remaining
            procInstStmt.executeBatch();
            procStatusStmt.executeBatch();
            conn.commit();
            
            // Clean up resources
            procInstStmt.close();
            procStatusStmt.close();
            
        } catch (Exception e) {
            System.err.println("Error populating database: " + e.getMessage());
            e.printStackTrace();
            
            // Try to rollback if there was an error
            try {
                if (conn != null) conn.rollback();
            } catch (Exception rollbackEx) {
                System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
            }
        } finally {
            // Close connection
            try {
                if (conn != null) conn.close();
            } catch (Exception closeEx) {
                System.err.println("Error closing database connection: " + closeEx.getMessage());
            }
        }
    }
    
    /**
     * Placeholder for serializing variables to byte array
     * In a real implementation, this would use the appropriate serialization method
     * as used by the actual CWS codebase
     */
    private static byte[] serializeVariables(Map<String, String> variables) {
        try {
            // Simple string serialization for demo purposes
            // In real implementation, would use FST serialization like in SchedulerDbService
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StringBuilder sb = new StringBuilder();
            
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
            
            byte[] bytes = sb.toString().getBytes();
            baos.write(bytes);
            return baos.toByteArray();
            
        } catch (Exception e) {
            System.err.println("Error serializing variables: " + e.getMessage());
            return new byte[0];
        }
    }
}