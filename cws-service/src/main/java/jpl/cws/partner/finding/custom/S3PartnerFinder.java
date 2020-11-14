package jpl.cws.partner.finding.custom;

import java.util.List;
import java.util.Map;

/**
 * Interface for S3 group finding.
 * 
 * Actual partner finder implementations will implement these methods.
 * 
 */
public interface S3PartnerFinder {

	/**
	 * Initializes the finder with context information
	 * @param context
	 */
	void initialize(Map<String, Object> context);

	/**
	 * Retrieves the input partners for the specified input filename (s3ObjName).
	 * 
	 * @param s3ObjName
	 * @param s3BucketName
	 * 
	 * @return List of File objects that make up the entire set
	 * of input partners.  This list includes the specified file.
	 */
	List<String> getInputPartners(String s3ObjName, String s3BucketName) throws Exception;
}