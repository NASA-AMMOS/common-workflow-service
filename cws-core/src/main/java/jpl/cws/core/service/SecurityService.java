package jpl.cws.core.service;

import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.IdentityService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;

import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.core.log.CwsEmailerService;
import jpl.cws.core.web.CwsToken;

public class SecurityService {
	private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

	@Autowired private SchedulerDbService schedulerDbService;
	@Autowired private IdentityService identityService;
	@Autowired private CwsEmailerService cwsEmailerService;
	
	@Value("${cws.token.expiration.hours}") private Integer prop_TokenExpirationInHours;
	
	private String projectWebappRoot;

	private static Map<String, CwsToken> cwsTokensMap = new HashMap<String, CwsToken>();

	/**
	 * Only used in LDAP and Camunda security schemes
	 */
	public boolean checkCredentialsViaIdentityService(String username,
			String passwordOrToken) {
		log.debug("checking identity via identity service for user: '" + username + "'...");
		try {
			return identityService.checkPassword(username, passwordOrToken);
		}
		catch (Throwable e) {
			cwsEmailerService.sendNotificationEmails("CWS LDAP Authentication Error", "Severe Error!\n\nError during authentication.\n\nDetails: " + e.getMessage());
			log.error("Authentication service error.", e);
			
			throw new RuntimeException(e);
		}
	}
	

	
	
	/**
	 * Looks for a CWS token in-memory, and if not found there,
	 * falls back to looking in the database for it.
	 * If found, the token is returned.
	 * 
	 */
	public CwsToken getCwsToken(String cwsToken, String username) {
		log.trace("getCwsToken(" + cwsToken + ", " + username + ")");
		
		// Attempt to get from in-memory map first..
		//
		String inMemKey = username + "_" + cwsToken;
		CwsToken tokenInMem = cwsTokensMap.get(inMemKey);
		if (tokenInMem != null) {
			return tokenInMem; // found in memory, so return
		}
		
		// If not found in memory, then get from database..
		//
		log.debug("Falling back to DB (wasn't cached in-memory) to get CWS token (" + cwsToken + ", " + username + ")...");
		try {
			if (username != null) {
				Map<String, Object> row = schedulerDbService.getCwsToken(cwsToken, username);
				CwsToken tokenFromDb = new CwsToken(
						cwsToken,
						row.get("username").toString(),
						(Timestamp) row.get("expiration_time"));
				
				// Update in-memory tokens
				//
				log.debug("adding cwsToken (" + tokenFromDb + ") entry to cwsSessionIds map...");
				cwsTokensMap.put(inMemKey, tokenFromDb);
				log.trace("updated in-memory-tokens: " + cwsTokensMap);
				
				return tokenFromDb;
			}
			else {
				for (Map<String, Object> row : schedulerDbService.getAllCwsTokens()) {
					if (row.get("token").toString().equals(cwsToken)) {
						CwsToken tokenFromDb = new CwsToken(
								cwsToken,
								row.get("username").toString(),
								(Timestamp) row.get("expiration_time"));
						
						// Update in-memory tokens
						//
						log.debug("adding cwsToken (" + tokenFromDb + ") entry to cwsSessionIds map...");
						cwsTokensMap.put(inMemKey, tokenFromDb);
						log.trace("updated in-memory-tokens: " + cwsTokensMap);
						
						return tokenFromDb;
					}
				}
			}
			
			log.debug("No CWS token found for (cwsToken='" + cwsToken
					+ "', username='" + username + "') in memory or DB.");
			return null;
		} catch (EmptyResultDataAccessException e) {
			log.debug("No CWS token found for (cwsToken='" + cwsToken
					+ "', username='" + username + "') in memory or DB.");
			return null;
		}
	}
	
	
	/**
	 * 
	 */
	public void addNewCwsTokenToDb(String cwsToken, String username) {
		log.info("adding new token to db: " + cwsToken + ", " + username);
		Timestamp expirationTime = new Timestamp(DateTime.now().plusHours(prop_TokenExpirationInHours).getMillis());
		schedulerDbService.insertCwsToken(cwsToken, username, expirationTime);
	}
	
	
	/**
	 * 
	 */
	public void deleteCwsToken(String cwsToken, String username) {
		schedulerDbService.deleteCwsToken(cwsToken, username); // delete from DB
		clearCwsTokenMapEntry(cwsToken, username);             // remove from in-memory cache
	}
	
	
	/**
	 * 
	 */
	public void clearCwsTokenMapEntry(String cwsToken, String username) {
		String inMemKey = username + "_" + cwsToken;
		if (cwsTokensMap.get(inMemKey) == null) {
			log.trace("nothing to clear from cwsSessionIds for '" + inMemKey
					+ "'! (already empty or non-existent)");
		} else {
			CwsToken removed = cwsTokensMap.remove(inMemKey);
			if (removed == null) {
				log.warn("nothing was removed from cwsSessionsIds!");
			} else {
				log.debug("removed cwsToken: " + removed
						+ " from cwsSessionsIds.");
			}
		}
		log.trace("sessionIds: " + cwsTokensMap);
	}
	
	
	/**
	 * 
	 */
	public void populateInMemoryCwsTokenMapFromDb() {
		log.debug("Populating CWS tokens from Database, into memory...");
		log.warn("FIXME: this is uncessary if we are in a non-CWS token authentication scheme");

		for (Map<String, Object> row : schedulerDbService.getAllCwsTokens()) {
			String username = row.get("username").toString();
			String cwsToken = row.get("token").toString();
			String inMemKey = username + "_" + cwsToken;
			CwsToken tokenFromDb = new CwsToken(cwsToken, username,
					(Timestamp) row.get("expiration_time"));
			cwsTokensMap.put(inMemKey, tokenFromDb);
			log.info("populated a token for username: '" + username + "'.");
		}
	}
	
	public String getProjectWebappRoot() {
		return projectWebappRoot;
	}
	public void setProjectWebappRoot(String projectWebappRoot) {
		this.projectWebappRoot = projectWebappRoot;
	}
	
}
