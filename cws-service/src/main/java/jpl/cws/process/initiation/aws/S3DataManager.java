package jpl.cws.process.initiation.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

/**
 * @author ghollins, jwood, ztaylor
 */
public class S3DataManager implements AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(S3DataManager.class);

	public static final int OLD_SECONDS_BACK_THRESHOLD = 120;

	private Region regionUsed;

	private S3Client s3;

	public S3DataManager(String region) {
		init(region); // sets defaults
	}

	public void init(String region) {
		s3 = S3Client.builder().region(Region.of(region)).build();
		regionUsed = Region.of(region);
	}

	@Override
	public void close() throws Exception {
		if (s3 != null) {
			s3.close();
		}
	}

	public S3Client getClient() {
		return s3;
	}
	
	
	public void setEndpoint(String url) {
		s3 = S3Client.builder().region(regionUsed)
				.endpointOverride(URI.create(url))
				.build();
	}
	
	
	public boolean isObjectExist(String bucketName, String key) {
		try {
			return getObjectMetadata(bucketName, key) != null;
		}
		catch(Exception e) {
			// TODO: only warn if a real exception, not simply "not found" (which can be a nominal case)
			log.warn("Exception while isObjectExist(bucketName='"+ bucketName + "', key='" + key + "') : " + e.getMessage());
			return false;
		}
	}
	
	
	/**
	 * Gets a HeadObjectReponse for the specified object.
	 * 
	 */
	public HeadObjectResponse getObjectMetadata(String bucketName, String key) {

			HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();

			HeadObjectResponse headObjectResponse = s3.headObject(headObjectRequest);

			return headObjectResponse;
	}
	
	
	/**
	 * Gets an object from S3, and optionally (if destinationPath is specified) downloads it to a file.
	 * Return CwsObjectMetadata representing the object and the downloaded path.
	 * 
	 * Throws an AmazonS3Exception if object does not exist.
	 * 
	 */
	public File getObject(String bucketName, String key, String targetDirectory) throws Exception {
		File file = null;
		String finalFilePath = null;
		log.info("KEYS: " + s3);
		try {
			file = new File(targetDirectory + "/" + key.replace('/',  '_'));
			//file = File.createTempFile("file", null);

			// TODO: these three calls are probably not necessary
			file.setWritable(true);
			file.setExecutable(true);
			file.setReadable(true);

			s3.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build(),
					ResponseTransformer.toFile(file));

			log.info("S3 file downloaded to : " + file.getAbsolutePath() + ". (exists = " + file.exists() + ")");
		}
		catch (NoSuchKeyException e) {
			log.warn("S3 object with key of '" + key + "' NOT FOUND");
			throw e;
		}
		catch (Throwable t) {
			log.error("Throwable while getObject(" +bucketName + ", " + key + ", " + targetDirectory + ")", t);
			throw t;
		}

		return file;
	}
	
	
	public List<Bucket> listBuckets() {
		ListBucketsResponse resp = s3.listBuckets();
		return resp.buckets();
	}
	
	public void createBucket(String bucketName) {
		// Create bucket
		CreateBucketRequest createBucketRequest = CreateBucketRequest
				.builder()
				.bucket(bucketName)
				.createBucketConfiguration(CreateBucketConfiguration.builder()
						//.locationConstraint(region.id())
						.build())
				.build();
		CreateBucketResponse resp = s3.createBucket(createBucketRequest);
	}
	
	public void putObject(String bucket, String key, File file) {
		s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
				RequestBody.fromFile(file));
	}

	/**
	 * Returns true, if file is older than OLD_SECONDS_BACK_THRESHOLD
	 *
	 */
	public boolean isOld(String bucket, String pathPrefix, String fileNamePrefix, String extension) {
		List<String> ret = new ArrayList<>();
		try {
			log.debug("isOld( S3 bucket=" + bucket
					+ ", pathPrefix=" + pathPrefix
					+ ", prefix=" + fileNamePrefix
					+ ", ext= " + extension + " )");

			ListObjectsV2Request req = null;
			ListObjectsV2Response resp = null;

			if (pathPrefix != null) {
				req = ListObjectsV2Request.builder()
						.bucket(bucket)
						.prefix(pathPrefix)
						.build();
			}
			else {
				req = ListObjectsV2Request.builder()
						.bucket(bucket)
						.build();
			}

			do {
				resp = s3.listObjectsV2(req);

				for (S3Object content : resp.contents()) {
					String key = content.key();
					int slashIdx = key.lastIndexOf("/");
					String fileName;
					if (slashIdx == -1) { fileName = key; }
					else { fileName = key.substring(slashIdx+1); }

					//log.debug("FILENAME: " + fileName);
					if (fileName.startsWith(fileNamePrefix) && fileName.endsWith(extension)) {
						Instant lastMod = content.lastModified();
						Instant recently = Instant.now().minusSeconds(OLD_SECONDS_BACK_THRESHOLD);
						log.debug("CHECKING: " + lastMod + " / " + recently + " " + lastMod.isBefore(recently));
						return lastMod.isBefore(recently);
					}
				}

				if (pathPrefix != null) {
					req = ListObjectsV2Request.builder()
							.bucket(bucket)
							.prefix(pathPrefix)
							.continuationToken(resp.nextContinuationToken())
							.build();
				}
				else {
					req = ListObjectsV2Request.builder()
							.bucket(bucket)
							.continuationToken(resp.nextContinuationToken())
							.build();
				}

			} while (resp.isTruncated());

		} catch (AwsServiceException ase) {
			System.out.println("Caught an AmazonServiceException, " +
					"which means your request made it " +
					"to Amazon S3, but was rejected with an error response " +
					"for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("Error Details:    " + ase.awsErrorDetails());
		} catch (SdkClientException ace) {
			System.out.println("Caught an AmazonClientException, " +
					"which means the client encountered " +
					"an internal error while trying to communicate" +
					" with S3, " +
					"such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
		catch (Throwable t) {
			log.error("Unexpected Throwable in listObjectKeys", t);
		}

		return false;
	}


	public List<String> listObjectKeys(String bucket, String pathPrefix, String fileNamePrefix, String extension) {
		List<String> ret = new ArrayList<>();
		try {
			log.debug("Listing objects for S3 bucket=" + bucket
					+ ", pathPrefix=" + pathPrefix
					+ ", prefix=" + fileNamePrefix
					+ ", ext= " + extension);

			ListObjectsV2Request req = null;
			ListObjectsV2Response resp = null;
			if (pathPrefix != null) {
				req = ListObjectsV2Request.builder()
						.bucket(bucket)
						.prefix(pathPrefix)
						.build();
			}
			else {
				req = ListObjectsV2Request.builder()
						.bucket(bucket)
						.build();
			}

			do {
				resp = s3.listObjectsV2(req);

				for (S3Object content : resp.contents()) {
					String key = content.key();
					int slashIdx = key.lastIndexOf("/");
					String fileName;
					if (slashIdx == -1) { fileName = key; }
					else { fileName = key.substring(slashIdx+1); }

					if (fileName.startsWith(fileNamePrefix)) {
						// limit to extension, if specified
						if (extension != null) {
							if(fileName.endsWith("." + extension)) {
								ret.add(key);
							}
						}
						else {
							ret.add(key);
						}
					}
				}

				// Build continuation request
				//
				if (pathPrefix != null) {
					req = ListObjectsV2Request.builder()
							.bucket(bucket)
							.prefix(pathPrefix)
							.continuationToken(resp.nextContinuationToken())
							.build();
				}
				else {
					req = ListObjectsV2Request.builder()
							.bucket(bucket)
							.continuationToken(resp.nextContinuationToken())
							.build();
				}

			} while (resp.isTruncated());

		} catch (AwsServiceException ase) {
			System.out.println("Caught an AmazonServiceException, " +
					"which means your request made it " +
					"to Amazon S3, but was rejected with an error response " +
					"for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("Error Details:    " + ase.awsErrorDetails());
//			System.out.println("AWS Error Code:   " + ase.getErrorCode());
//			System.out.println("Error Type:       " + ase.getErrorType());
//			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (SdkClientException ace) {
			System.out.println("Caught an AmazonClientException, " +
					"which means the client encountered " +
					"an internal error while trying to communicate" +
					" with S3, " +
					"such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
		catch (Throwable t) {
			log.error("Unexpected Throwable in listObjectKeys", t);
		}

		return ret;
	}
	

	/**
	 * Moves a file atomically to the specified directory.
	 * 
	 * @return new Path of file after move
	 */
	private Path moveFile(String sourceFilename, String targetFilename) throws Exception {
		Path source = Paths.get(sourceFilename);
		Path target = Paths.get(targetFilename);
		
		System.out.println("MOVING: " + source + " ---> " + target);
		
		Path movedFile = null;
		try {
			movedFile = Files.move(source, target, ATOMIC_MOVE);
			System.out.println("MOVED: " + source + " ---> " + target);
		}
		catch (NoSuchFileException e) {
			System.out.println("NoSuchFileException while moving file to directory (" +
					source + " ---> " + target + ").  This probably means another thread beat us to moving this file.");
			return null;
		}
		catch (Exception e) {
			System.out.println("Unexpected problem occurred while moving file (" +
					source + " ---> " + target + ")");
			throw e;
		}
		return movedFile;
	}
	
}
