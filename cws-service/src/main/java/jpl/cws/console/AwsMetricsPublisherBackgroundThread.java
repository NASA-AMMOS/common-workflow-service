package jpl.cws.console;

import jpl.cws.core.db.SchedulerDbService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AwsMetricsPublisherBackgroundThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(AwsMetricsPublisherBackgroundThread.class);

	@Autowired private SchedulerDbService schedulerDbService;
	@Value("${cws.enable.cloud.autoscaling}") private String cwsEnableCloudAutoscaling;
	@Value("${cws.aws.cloudwatch.endpoint}") private String awsCloudWatchEndpoint;
	@Value("${cws.aws.putmetric.namespace}") private String awsPutMetricNamespace;
	@Value("${cws.metrics.publishing.interval}") private String cwsMetricsPublishingInterval;

	private CloudWatchClient cloudWatch;
	private int threadInterval;
	
	private static final String QUEUE_MAX_PENDING_DURATION_METRIC_NAME = "queueMaxPendingDuration";
	private static final String NUM_ACTIVE_WORKERS_METRIC_NAME = "numActiveWorkers";
	private static final int MILLIS_PER_SEC = 1000;
			
	public void run() {
		// Only run this background thread if cloud auto-scaling is enabled
		//
		if (!cwsEnableCloudAutoscaling.equals("true")) {
			log.error("cwsEnableCloudAutoscaling = " + cwsEnableCloudAutoscaling + 
					  ", which is unexpected. Aborting thread!");
			return;
		}

		log.debug("AwsMetricsPublisherBackgroundThread starting...");
		
		try {
			threadInterval = Integer.parseInt(cwsMetricsPublishingInterval);
			if (threadInterval <= 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			log.error("Invalid value for cwsMetricsPublishingInterval: " + cwsMetricsPublishingInterval +
					  ", value must be a positive integer.");
			return;
		}
		
		log.debug("Metrics publishing interval set to " + cwsMetricsPublishingInterval + " seconds.");
		
		try {
			initCloudWatch();
		} catch (Exception e) {
			log.error("Failed to initialize the CloudWatch client", e);
			return;
		}

		while (true) {
			try {
				sleep(threadInterval * MILLIS_PER_SEC);
				
				// Publish the max queue pending time metric to AWS
				Long maxPendingQueueTime = getMaxPendingQueueTime();
				publishMetric(QUEUE_MAX_PENDING_DURATION_METRIC_NAME,
					maxPendingQueueTime == null ? 0.0 : maxPendingQueueTime.doubleValue());
				
				// Publish the number of workers metric to AWS
				int numWorkers = getNumActiveWorkers();
				publishMetric(NUM_ACTIVE_WORKERS_METRIC_NAME, new Double(numWorkers));
				
			} catch (InterruptedException e) {
				log.warn("AwsMetricsPublisherBackgroundThread interrupted. Must be shutting down..");
				
				// It appears that AWS spawns a thread that needs be be shutdown manually
				// FIXME:  when this code was upgraded to the AWS v2.x.x version, of the
				//         library, the shutdown() method didn't seem to exist anymore...
				//software.amazon.awssdk.http.apache.internal.conn.IdleConnectionReaper.shutdown();
				
				break;
			}
		}

		log.debug("AwsMetricsPublisherBackgroundThread stopping...");
	}
	
	/**
	 * Obtains credentials and initializes the AWS CloudWatch Client object
	 * 
	 */
	private void initCloudWatch() throws Exception {
		log.trace("Initializing AWS CloudWatch client");

		cloudWatch = CloudWatchClient.builder().build();

		// Get the signing region from the provided endpoint
		// AWS endpoints should be of form <service>.<region>.amazonaws.com
		String[] parsedEndpoint = awsCloudWatchEndpoint.split("\\.");
		
		if (parsedEndpoint.length != 4) {
			throw new Exception("Received a malformed CloudWatch endpoint: " + awsCloudWatchEndpoint);
		}

		String signingRegion = parsedEndpoint[1];
		log.trace("Set AWS signing region to '" + signingRegion + "'.");

//		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(awsCloudWatchEndpoint, signingRegion);
//		clientBuilder.setEndpointConfiguration(endpointConfiguration);

		log.trace("Set AWS CloudWatch client endpoint to '" + awsCloudWatchEndpoint + "'.");

		//cloudWatch = clientBuilder.build();
		log.trace("Created AWS CloudWatch client: " + cloudWatch);
	}
	
	
	/**
	 * Returns the max pending queue time across all "pending" rows
	 * 
	 */
	private Long getMaxPendingQueueTime() {
		Timestamp now = new Timestamp(DateTime.now().getMillis());
		
		// Get list of scheduled rows in the 'pending' state
		List<Map<String,Object>> pendingRows = schedulerDbService.getPendingProcessInstances();
		if (pendingRows.isEmpty()) {
			log.trace("No 'pending' rows found in DB.");
			return null;
		}
		
		// Get list of enabled process definitions (acceptable by one or more workers)
		List<String> acceptableProcDefs = schedulerDbService.getAcceptableProcDefKeys();
		log.debug(pendingRows.size() + " 'pending' rows found in DB, " + acceptableProcDefs.size() + 
				  " process defs are being accepted at this time.");
		if (acceptableProcDefs.isEmpty()) {
			return null;
		}
		
		long tsNow = now.getTime();
		
		Long maxPendingTime = null;
		for (Map<String,Object> row : pendingRows) {
			String procDefKey = (String)row.get("proc_def_key");
			if (!acceptableProcDefs.contains(procDefKey)) {
				log.trace("Ignoring a '" + procDefKey + "' process in max queue time calculation, " +
			              "since it's not being accepted right now..");
				continue; // don't eval this one, since nothing is accepting it currently
			}
			Timestamp createdTime = (Timestamp)row.get("created_time");
			
			// get the difference in time between now and when row was created
			long timeInPending = tsNow - createdTime.getTime();
			
			if (maxPendingTime == null || timeInPending > maxPendingTime) {
				maxPendingTime = timeInPending;
			}
		}
		
		return maxPendingTime;
	}
	
	
	/**
	 * Returns the number of active workers
	 * 
	 */
	private int getNumActiveWorkers() {
		int numWorkers = 0;
		List<Map<String,Object>> workerRows = schedulerDbService.getWorkers();
		for (Map<String,Object> workerRow : workerRows) {
			Object statusObj = workerRow.get("status");
			if (statusObj != null) {
				if (statusObj.equals("up")) {
					numWorkers++;
				}
			}
			else {
				log.warn("Worker status was null for worker " + workerRow.get("id"));
			}
		}
		return numWorkers;
	}
	
	
	/**
	 * Publishes metric to AWS
	 * 
	 */
	private void publishMetric(String metricName, Double metricValue) {
		if (metricValue == null) {
			metricValue = new Double(0); // still publish a data point..
		}
		
		try {
			MetricDatum datum = MetricDatum.builder()
					.metricName(metricName)
					.unit(StandardUnit.NONE)
					.timestamp(Instant.now())
					.value(metricValue)
					.build();

			PutMetricDataRequest putMetricDataRequest = PutMetricDataRequest.builder()
					.namespace("awsPutMetricNamespace")
					.metricData(datum).build();

			log.trace("Created new PutMetricDataRequest: " + putMetricDataRequest);

			// Publish the MetricDatum to AWS
			log.debug("Publishing [ '" + metricName + "' = " + metricValue + "] to AWS...");
			cloudWatch.putMetricData(putMetricDataRequest);

			log.trace("Published MetricDatum to AWS.");
		}
		catch (Exception e) {
			log.error("Problem while publishing metric to AWS", e);
		}
	}
}
