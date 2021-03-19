package jpl.cws.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpl.cws.core.web.CwsSecurityFilter;
import jpl.cws.core.web.WebUtils;
import jpl.cws.core.web.WebUtils.RestCallResult;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.Expression;

import com.google.gson.Gson;

/**
 * Built-in task that executes a REST GET operation.
 * 
 * REQUIRED parameters: -- url
 * 
 * OPTIONAL parameters: -- mimeType (defaults to 'text/plain')
 * 
 */
public class RestGetTask extends CwsTask {

	public static final boolean DEFAULT_THROW_ON_BAD_RESPONSE = true;
	public static final boolean DEFAULT_ALLOW_INSECURE_REQUESTS = false;

	public static final String DEFAULT_CWS_COOKIE_NAME = CwsSecurityFilter.CWS_TOKEN_COOKIE_NAME;

	private static final String CWS_VARS_FROM_STDOUT_REGEX = "\\\"?cwsVariables\\\"?\\s*:\\s*(\\{(.+?)\\})";
	private static Pattern p = Pattern.compile("^"+CWS_VARS_FROM_STDOUT_REGEX+"$");

	private Expression url;
	private Expression throwOnBadResponse;
	private Expression acceptType;
	private String urlString;
	private boolean throwOnBadResponseBoolean;
	private String acceptTypeString;
	private Expression allowInsecureRequests;
	private boolean allowInsecureRequestsBoolean;
	private Expression httpAuthUsername;
	private String httpAuthUsernameString;
	private Expression httpAuthPassword;
	private String httpAuthPasswordString;
	
	// CWS token security scheme variables
	//
	private Expression cwsTokenFileLocation;   // absolute path to CWS token file
	private String cwsTokenFileLocationString;
	private Expression cwsCookieName;       // cookie name that CWS expects
	private String cwsCookieNameString;
	private Expression cwsCookieValue;      // cookie value -- this is the CWS "token"
	private String cwsCookieValueString;

	
	public RestGetTask() {
		log.trace("RestGetTask constructor...");
	}

	@Override
	public void initParams() throws Exception {
		urlString = getStringParam(url, "url");
		allowInsecureRequestsBoolean = getBooleanParam(allowInsecureRequests, "allowInsecureRequests", DEFAULT_ALLOW_INSECURE_REQUESTS);
		httpAuthUsernameString = getStringParam(httpAuthUsername, "httpAuthUsername", null);
		httpAuthPasswordString = getStringParam(httpAuthPassword, "httpAuthPassword", null);
		throwOnBadResponseBoolean = getBooleanParam(throwOnBadResponse, "throwOnBadResponse", DEFAULT_THROW_ON_BAD_RESPONSE);
		acceptTypeString = getStringParam(acceptType, "acceptType", null);

		// CWS token security scheme variables
		//
		cwsTokenFileLocationString = getStringParam(cwsTokenFileLocation, "cwsTokenFileLocation", null);
		cwsCookieNameString = getStringParam(cwsCookieName, "cwsCookieName", DEFAULT_CWS_COOKIE_NAME);
		cwsCookieValueString = getStringParam(cwsCookieValue, "cwsCookieValue", null);
	}
	

	@Override
	public void executeTask() throws Exception {
		log.info("Starting REST GET (URL = " + urlString + ")");

		// if necessary variables are set,
		// then get the token (CWS or CAM) to use as a cookie value
		//
		String cookieToUse = null;
		if (cwsTokenFileLocationString != null) {
			cwsCookieValueString = getCwsTokenFromFile();
			cookieToUse = cwsCookieNameString + "=" + cwsCookieValueString;
		}
		else {
			log.debug("not using CWS token approach -- and that's okay.");
		}
		
		// Make the REST GET
		//
		RestCallResult result;
		if (httpAuthUsername == null) {
			// Unauthenticated GET
			result = WebUtils.restCall(
					urlString, "GET",
					null, // no POST args, since we are making a GET call
					cookieToUse,
					acceptTypeString,
					null);
		} else {
			// Authenticated GET
			result = WebUtils.restCall(
					urlString, "GET",
					null, // no POST args, since we are making a GET call
					cookieToUse,
					acceptTypeString,
					null,
					httpAuthUsernameString,
					httpAuthPasswordString);
		}
		
		int responseCode = result.getResponseCode();
		log.info("httpStatusCode: " + responseCode);
		this.setOutputVariable("httpStatusCode", String.valueOf(responseCode));
		
		String response = result.getResponse();
		log.trace("response: " + response);
		
		if (responseCode != 200 && responseCode != 303) {
			if (throwOnBadResponseBoolean) {
				throw new BpmnError(result.getResponseMessage() + " (" + responseCode + ")");	
			}
			else {
				log.warn("got bad response (responseCode = " + responseCode + "), but not throwing BpmnError, because throwOnBadResponseBoolean is false.");
			}
		}
	
		this.setOutputVariable("response", response);
		setVariablesFromResponse(response);
	
		log.info("Finished REST GET operation.");
	}


	/**
	 * 
	 */
	private String getCwsTokenFromFile() throws Exception {
		BufferedReader br = null;
		try {
			FileReader fr = new FileReader(new File(cwsTokenFileLocationString));
			br = new BufferedReader(fr);
			return br.readLine(); // read and return the token
			
			// FIXME:  need to parse netscape cookie file format!!
		}
		catch (Exception e) {
			log.error("Exception while getting/reading token file (" +
					cwsTokenFileLocationString + ")", e);
			throw e;
		}
		finally {
			if (br != null) {
				br.close();
			}
		}
	}


	/**
	 * Looks in the response for variables to set
	 * 
	 * NOTE: an initial pass has already been made at optimizing the performance
	 *       of this method.
	 */
	private void setVariablesFromResponse(String response) {
		Matcher m = p.matcher(response);
		if (m.matches()) {
			if(log.isTraceEnabled()) { log.trace("GOING TO PARSE: " + m.group(1)); }
			Map<String, Object> jsonAsMap = new Gson().fromJson(m.group(1), Map.class);
			if(log.isTraceEnabled()) { log.trace("JSON MAP: " + new Gson().fromJson(m.group(1), Map.class)); }
			for (Entry<String, Object> entry : jsonAsMap.entrySet()) {
				if(log.isTraceEnabled()) { log.trace("STDOUT VARIABLE: [" + entry.getKey() + "-->" + entry.getValue() + "]"); }
				setOutputVariable(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public Expression getUrl() {
		return url;
	}

	public void setUrl(Expression url) {
		this.url = url;
	}
	
	public Expression getAllowInsecureRequests() {
		return allowInsecureRequests;
	}
	
	public void setAllowInsecureRequests(Expression allowInsecureRequests) {
		this.allowInsecureRequests = allowInsecureRequests;
	}

	public Expression getHttpAuthUsername() {
		return httpAuthUsername;
	}

	public void setHttpAuthUsername(Expression httpAuthUsername) {
		this.httpAuthUsername = httpAuthUsername;
	}

	public Expression getHttpAuthPassword() {
		return httpAuthPassword;
	}

	public void setHttpAuthPassword(Expression httpAuthPassword) {
		this.httpAuthPassword = httpAuthPassword;
	}
	
	public Expression getThrowOnBadResponse() {
		return throwOnBadResponse;
	}

	public void setThrowOnBadResponse(Expression throwOnBadResponse) {
		this.throwOnBadResponse = throwOnBadResponse;
	}

	public Expression getAcceptType() {
		return acceptType;
	}

	public void setAcceptType(Expression acceptType) {
		this.acceptType = acceptType;
	}
}
