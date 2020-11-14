package jpl.cws.core.web;

import java.sql.Timestamp;

public class CwsToken {

	private String cwsToken;
	private String username;
	private Timestamp expirationTime;
	
	public CwsToken(String cwsToken, String username, Timestamp expirationTime) {
		super();
		this.cwsToken = cwsToken;
		this.username = username;
		this.expirationTime = expirationTime;
	}

	public String getCwsToken() {
		return cwsToken;
	}

	public String getUsername() {
		return username;
	}

	public Timestamp getExpirationTime() {
		return expirationTime;
	}
	
	/**
	 * 
	 */
	public boolean isExpired() {
		if (expirationTime == null) {
			throw new IllegalStateException("expirationTime was null!");
		}
		if ((System.currentTimeMillis() - expirationTime.getTime()) > 0) {
			return true;
		}
		return false;
	}
	
	public String toString() {
		return "[" + cwsToken + ", " + username + ", " + expirationTime + "]";
	}
}
