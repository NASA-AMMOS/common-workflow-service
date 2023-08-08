package jpl.cws.core;

public class CmdLineInputFields {

	public String command = null;
	public String workingDir = null;
	public String successfulValues = null;
	public String exitCodeEvents = null;
	public boolean throwOnFailures;
	public boolean throwOnTruncatedVariable;
	public long timeout = 0;                    // in seconds
	public int retries = 0;
	public int retryDelay = 0;                  // in milliseconds
}
