package jpl.cws.core.code;

import java.util.*;
import java.util.regex.*;
import java.io.*;


public class CustomMethods {
	
	public static final String AWS_REGION = "us-gov-west-1";

	public String getRandUuid() {
		return UUID.randomUUID().toString();
	}

	public long randLong() {
		return (long)(Math.floor(Math.random() * 28) + 2) * 1000l;
	}
	
	/** 
	 * Retrieves the value of the supplied envVar
	 * 
	 * @param envVar
	 * @returns value of environment variable
	 */
	public String getEnv(String envVar) {
		return System.getenv(envVar);
	}
	
}

