package jpl.cws.process.initiation.aws;

import com.google.gson.Gson;
import jpl.cws.core.log.CwsEmailerService;
import jpl.cws.process.initiation.CwsProcessInitiator;
import jpl.cws.process.initiation.InitiatorsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This thread subscribes to an AWS SQS URL, and when a new message arrives,
 * will route the message to the appropriate initiators.
 * 
 * The set of appropriate initiators is defined by a mapping that pairs
 * regular expressions representing S3 object names, to their initiators.
 * 
 * More than one initiator can match, and be dispatched to.
 * 
 * This thread is launched via the adaptation Spring context file loaded at CWS startup.
 *
 * @author ghollins, ztaylor
 *
 */
public class SQSDispatcherThread extends Thread implements InitializingBean {
	private static Logger log = LoggerFactory.getLogger(SQSDispatcherThread.class);

	@Value("${aws.default.region}") private String aws_default_region;
	@Value("${cws.console.hostname}") private String consoleHostname;
	@Value("${cws.console.port.ssl}") private String consoleSslPort;

	// The SQS URL -- this is what this initiator "watches" for incoming messages
	//
	@Value("${aws.sqs.dispatcher.sqsUrl}") private String sqsUrl;

	// The maximum number of messages to consume from SQS. Valid values are 1 to 10, defaults to 1.
	//
	@Value("${aws.sqs.dispatcher.msgFetchLimit}") private Integer msgFetchLimit;

	@Autowired private InitiatorsService initiatorsService;
    @Autowired private CwsEmailerService cwsEmailerService;

	private SqsClient sqs;
	private long lastClientRefreshTime;
	private static final int TOKEN_REFRESH_FREQUENCY = 60 * 10 * 1000; // 10 minutes in milliseconds
	private static final Integer SQS_CLIENT_WAIT_TIME_SECONDS = 20;
	private Gson gson;

	//
	// Mapping of initiator ID to S3 object name patterns.
	// This mapping is loaded from the XML specified in the Initiators page.
	//
	private Map<String,HashSet<String>> dispatcherMap;
	static final Object dispatcherMapLock = new Object();

	// Maximum number of simultaneous threads that may be dispatched before throttling occurs.
	//  If this value is met, then the SQS request rate will be throttled by the given amount until the
	//  system can catch up.
	//
	@Value("${aws.sqs.dispatcher.maxThreads}") private Integer maxThreads;

	// number of threads in messageHandlerThreadExecutor running at a given moment
	private AtomicInteger numberThreads = new AtomicInteger(0);

	private ExecutorService messageDeleterThreadExecutor = Executors.newFixedThreadPool(10);
	private ExecutorService messageHandlerThreadExecutor = Executors.newFixedThreadPool(20);

	private long avgMsgHandleTimeMillis = 100;

	public SQSDispatcherThread() {
		log.debug("SQSDispatcherThread ctor...........................................");
	}


	public boolean isValid() {
		//AmazonS3 s3Client = AmazonSQSClientBuilder.defaultClient();
		log.trace("isValid()...");
		
		if (sqsUrl          == null) { log.warn("sqsUrl not specifed!");           return false; }
		else { log.debug("sqsUrl = " + sqsUrl + " [OK]"); }
		
		if (dispatcherMap     == null) { log.warn("dispatcherMap not specifed!");  return false; }
		else { log.debug("dispatcherMap = " + dispatcherMap + " [OK]"); }
		
		return true;
	}
	
	
	@Override
	public void run() {
	    try {
            log.debug("SQSDispatcherThread STARTING...");
            gson = new Gson();

		    // See:  https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-jvm-ttl.html
		    java.security.Security.setProperty("networkaddress.cache.ttl" , "60");

			refreshAwsClient(true);

            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(sqsUrl)
					.waitTimeSeconds(SQS_CLIENT_WAIT_TIME_SECONDS)
					//.waitTimeSeconds(0) // long polling
                    .maxNumberOfMessages(msgFetchLimit)
					//.visibilityTimeout(30)
                    .build();

            int iteration = 0;

            // While loop that repeatedly fetches messages
            // and delegates them to initiators.
            //
            while (true) {
				iteration++;
				if (iteration++ >= (Integer.MAX_VALUE-1)) { iteration = 0; }

				// periodically clear out
                if (Thread.interrupted()) {
                    log.warn("SQSDispatcherThread interrupted. Breaking out of infinite while loop...");
                    break;
                }

                try {
	                // Will throttle looping if max number of threads has been exceeded
	                if (numberThreads.get() > maxThreads) {
		                long actualThrottleMillis = (long)(1.1 * avgMsgHandleTimeMillis) * (numberThreads.get() - maxThreads);
		                log.warn("Throttling by {} ms ({}/{}) avgMsgHandleTime={}", actualThrottleMillis, numberThreads, maxThreads, avgMsgHandleTimeMillis);
		                Thread.sleep(actualThrottleMillis);
		                avgMsgHandleTimeMillis += 10; // backoff
		                continue;
	                }

                    log.trace("about to receive message...");
                    long t0 = System.currentTimeMillis();
                    refreshAwsClient(false);
                    List<Message> messages = sqs.receiveMessage(receiveMessageRequest).messages();
                    long t1 = System.currentTimeMillis();
	                log.debug("bufferedSqs.receiveMessage (in " + (t1 - t0) + "ms) [" +
			                messages.size() + " messages, " +
			                numberThreads.get() + " handlerThreads]");

                    if (messages.isEmpty()) {
                        log.trace("GOT " + messages.size() + " MESSAGE(S)");
                    } else {
                        if (messages.size() > msgFetchLimit) {
                            System.err.println("Unexpected number of messages! " + messages.size());
                        } else {
                            //
                            // For each received message
                            //
                            for (Message msg : messages) {
	                            numberThreads.incrementAndGet();
	                            handleMessageOnSeparateThread(msg);
                            }
                        }
                    }
                } catch (Throwable t) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.warn("SQSDispatcherThread interrupted. Breaking out of infinite while loop...");
                        break;
                    }
                    log.error("Unexpected exception in SQSDispatcherThread processing loop, but continuing on...", t);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e1) {
                        log.warn("SQSDispatcherThread interrupted during sleep. Breaking out of infinite while loop...");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } // end while (true)

        }
        catch (Throwable t) {
            cwsEmailerService.sendNotificationEmails("SQSDispatcherThread Error", "Severe Error!\n\nHSQSDispatcherThread run threw an exception. This is an unexpected error and should never happen. Look at logs for more details. You may have to restart CWS.\n\nDetails: " + t.getMessage());
            throw t;
	    }
	    finally {
            log.error("SQSDispatcherThread initiator ending...");
            messageDeleterThreadExecutor.shutdown();
        }
	}


	/**
	 * Deletes message from the queue, on a separate thread.
	 *
	 */
	private void handleMessageOnSeparateThread(Message msg) {

		//
		// Push the handling of the message to another thread
		//
		messageHandlerThreadExecutor.execute(new Runnable() {
			public void run() {
				long d0 = System.currentTimeMillis();

				String msgBody = msg.body();
				log.debug("MSG BODY: " + msgBody);

				long t2 = System.currentTimeMillis();

				// Get JSON representation of message
				//
				Map<String, Object> jsonAsList = null;
				Object records = null;

				Object rec0 = null;
				try {
					jsonAsList = gson.fromJson(msgBody, Map.class);
					records = jsonAsList.get("Records");
					if (records == null) {
						Object messageObj = jsonAsList.get("Message"); // get SNS message instead
						Map<String, Object> messageJson = gson.fromJson(messageObj.toString(), Map.class);
						records = messageJson.get("Records"); // now go to Records
						if (records == null) {
							log.warn("Records is null");
							rec0 = messageJson;
						}
						else {
							rec0 = ((List<Object>) records).get(0);
						}

						if (rec0 == null) {
							throw new Exception("First record in message was not found!");
						}
					}
				} catch (Exception e) {
					log.error("Unable to parse message as JSON.  Deleting this message from queue, and moving on to next message...", e);
					deleteMessageFromQueueOnSeparateThread(msg);
					numberThreads.decrementAndGet();
					return;
				}

				// Now that we have JSON representation of message, process it...
				//
				try {
					if (rec0 == null) {
						rec0 = ((List<Object>) records).get(0);
					}

					log.debug("REC[0]: " + rec0);

					Map<String, Object> record = (Map<String, Object>) rec0;

					if (records != null && record != null && !record.get("eventName").toString()
							.contains("s3:ObjectRemoved:Delete")) {
						Object s3Obj = record.get("s3");
						Object s3ObjObj = ((Map<String, Object>) s3Obj).get("object");
						String s3ObjName = ((Map<String, Object>) s3ObjObj).get("key").toString();
						Object s3ObjBucket = ((Map<String, Object>) s3Obj).get("bucket");
						String s3BucketName = ((Map<String, Object>) s3ObjBucket).get("name").toString();

						long t3 = System.currentTimeMillis();
						log.debug("NEW S3 OBJECT: " + s3ObjName + " (parsed/extracted in " + (t3 - t2) + " ms)");
						//
						// Get list of matching initiators
						//
						HashSet<String> matchingInitiators = getMatchingInitiators(s3ObjName);

						//
						// If initiator(s) are applicable, then dispatch to them
						//
						if (matchingInitiators.isEmpty()) {
							log.debug("no matching initiators for S3 object: '" + s3ObjName + "'");
						} else {
							log.trace("found " + matchingInitiators.size() + " matching initiators.");
							for (String matchingInitiator : matchingInitiators) {
								dispatchToInitiator(matchingInitiator, s3ObjName, s3BucketName, msg);
							}
						}
					}
				} catch (Exception e) {
					log.error("error while processing message", e);
					numberThreads.decrementAndGet();
					return;
				}
				finally {
					deleteMessageFromQueueOnSeparateThread(msg);
				}

				int curThreads = numberThreads.decrementAndGet();
				long handleDuration = (System.currentTimeMillis() - d0);
				if (handleDuration > 100) {
					log.debug("Handled message (in " + (System.currentTimeMillis() - d0) + " ms) " +
							curThreads + " threads now active)");
				}

				// keep track of avg message handling duration...
				if (avgMsgHandleTimeMillis > handleDuration) { avgMsgHandleTimeMillis--; } else { avgMsgHandleTimeMillis++; }
				if (avgMsgHandleTimeMillis < 30) { avgMsgHandleTimeMillis = 30; } // floor
			}
		});

	}

	/**
	 * Deletes message from the queue, on a separate thread.
	 * 
	 */
	private void deleteMessageFromQueueOnSeparateThread(Message msg) {
		final String messageReceiptHandle = msg.receiptHandle();
		if (messageReceiptHandle != null) {
			
			//
			// Push the deleting of the SQS message to another thread
			//
			messageDeleterThreadExecutor.execute(new Runnable() {
				public void run() {
					log.trace("Done processing SQS message. ABOUT TO DELETE SQS MSG...");
					long d0 = System.currentTimeMillis();

                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                            .queueUrl(sqsUrl)
                            .receiptHandle(messageReceiptHandle)
                            .build();
                    sqs.deleteMessage(deleteRequest);

                    if ((System.currentTimeMillis() - d0) > 100) {
	                    log.debug("Deleted (slowly) message from SQS (in " + (System.currentTimeMillis() - d0) + " ms)");
                    }
				}
			});
		}
		else {
			log.error("messageReceiptHandle for message " + msg + " was null!  Not attempting SQS delete.");
		}
	}

	
	/**
	 * Refreshes client with new AWS credentials.
	 *
	 */
	private void refreshAwsClient(boolean forceRefresh) {
		if (forceRefresh ||
			lastClientRefreshTime == 0 ||
			((System.currentTimeMillis() - lastClientRefreshTime) > TOKEN_REFRESH_FREQUENCY)) {

			log.debug("About to refresh AWS SQS client...");

			if (sqs != null) {
				sqs.close();
			}

            sqs = SqsClient.builder()
                    .region(Region.of(aws_default_region))
                    .build();

			lastClientRefreshTime = System.currentTimeMillis(); // update timestamp
			log.debug("AWS credentials / client refreshed.");
		}
		else {
            log.debug("Refresh of AWS SQS client not necessary.");
        }
	}
	
	
	/**
	 * Refreshes the dispatcher map, based on what set of current S3Initiators are active.
	 */
	private void refreshDispatcherMap() {
		long t0 = System.currentTimeMillis();
		dispatcherMap.clear();

		// get set of current initiators
		Map<String, CwsProcessInitiator> initiators = initiatorsService.getProcessInitiators();

		for (Entry<String, CwsProcessInitiator> initiatorEntry : initiators.entrySet()) {
			CwsProcessInitiator cand = initiatorEntry.getValue();
			if (!(cand instanceof S3Initiator)) {
				log.trace("skipping non S3Initiator : " + cand);
				continue;
			}
			if (cand.isEnabled()) {
				//
				// Initiator is of type S3Initiator, and is enabled,
				// so add its patterns to the dispatcherMap.
				//
				S3Initiator initiator = (S3Initiator)cand;
				String initiatorId = initiator.getInitiatorId();
				Map<String,String> initiatorS3ObjPatterns = initiator.getS3ObjPatterns();
				HashSet<String> uniqueSetOfPatterns = new HashSet<>(initiatorS3ObjPatterns.values());
				dispatcherMap.put(initiatorId, uniqueSetOfPatterns);
			}
			else {
				log.trace("skipping putting initiator in dispatcherMap: " + cand);
			}
		} // end for each initiator

		long t1 = System.currentTimeMillis();

		log.trace("run refreshDispatcherMap in " + (t1-t0) + " ms. Map is: " + dispatcherMap);
		log.debug("run refreshDispatcherMap in " + (t1-t0) + " ms. " + dispatcherMap.size() + " initiators.");
	}
	
	
	/**
	 * Returns the relevant/matching initiators for the specified 
	 * S3 object name.
	 * 
	 */
	private HashSet<String> getMatchingInitiators(String s3ObjName) {
		HashSet<String> matchingInitiators = new HashSet<>();
		synchronized (dispatcherMapLock) {
			refreshDispatcherMap();
			for (Object initiatorId : dispatcherMap.keySet().toArray()) {
				HashSet<String> initiatorPatterns = dispatcherMap.get(initiatorId);
				for (String initiatorPattern : initiatorPatterns) {
					if (s3ObjName.matches(initiatorPattern)) {
						matchingInitiators.add(initiatorId.toString()); // found a match
						break; // optimization to avoid redundant adds to same initiator
					}
				}
			}
		}
		return matchingInitiators;
	}
	
	
	/**
	 * Dispatches S3 object event to an initiator (S3Initiator type initiator).
	 * The initiator will then make the determination as to whether it should actually
	 * schedule a process.
	 * 
	 */
	private void dispatchToInitiator(String initiatorId, String s3ObjName, String s3BucketName, Message msg) {
		log.debug("Dispatching s3ObjName '" + s3ObjName + "' to initiator: " + initiatorId);
		
		//
		// Get initiator, and validate that:
		//   * it's non-null
		//   * is of correct class
		//   * is valid, and
		//   * has same S3 bucket
		//
		S3Initiator initiator = null;
		try {
			Object initiatorCandidate = initiatorsService.getInitiator(initiatorId);
			if (initiatorCandidate == null) {
				log.error("initiator was null!  Not dispatching...");
				return;
			}
			if (!(initiatorCandidate instanceof S3Initiator)) {
				log.debug("initiator is of non-S3Initiator type!  Not dispatching...");
				return;
			}
			initiator = (S3Initiator)initiatorCandidate;
			if (!initiator.isValid()) {
				log.error("initiator is not valid!  Not dispatching...");
				return;
			}
			// initiator must be configured to use bucket this SQS event originated from
			if (!initiator.getS3BucketName().equals(s3BucketName)) {
				log.debug("initiator S3 bucket (" +
						initiator.getS3BucketName() + ") does not match dispatcher S3 bucket (" +
						s3BucketName + ").  Not dispatching...");
				return;
			}
		}
		catch (Exception e) {
			log.error("Unable to load initiator '" + initiatorId + "'.  Not dispatching...");
			return;
		}
		
		//
		// Initiator is good to go.
		// Ask initiator to make a determination whether all partners are found,
		// and if so, schedule a process.
		//
		try {
			initiator.reapplyProps();

			log.debug("got initiator: " + initiator);

			initiator.scheduleIfConditionsMet(s3ObjName);
		} catch (Exception e) {
			log.error("scheduleIfPartnersFound for (initiatorId='" + initiatorId + "', s3ObjName='" + s3ObjName + "') threw error", e);
		}
	}

	public String getSqsUrl() { return sqsUrl; }
	public void setSqsUrl(String sqsUrl) {
		this.sqsUrl = sqsUrl;
		log.trace("sqsUrl = " + sqsUrl);
	}

	public Map<String,HashSet<String>> getDispatcherMap() { return dispatcherMap; }
	public void setDispatcherMap(Map<String, HashSet<String>> dispatcherMap) {
		this.dispatcherMap = dispatcherMap;
	}
	
	
	public Integer getMsgFetchLimit() { return msgFetchLimit; }
	public void setMsgFetchLimit(Integer msgFetchLimit) {
		this.msgFetchLimit = msgFetchLimit;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		log.debug("afterPropertiesSet");
		if (isValid()) {
			this.start();
		}
		else {
			log.error("Not starting SQSDispatcherThread, since it's not valid!");
		}
	}

}
