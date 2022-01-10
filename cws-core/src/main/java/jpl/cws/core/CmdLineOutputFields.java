package jpl.cws.core;

import java.util.Date;

public class CmdLineOutputFields {
	public int exitCode;
	public boolean success;
	public String event = null;
	public String stdout;
	public String stderr;
	public Date lockedTime = null;
}
