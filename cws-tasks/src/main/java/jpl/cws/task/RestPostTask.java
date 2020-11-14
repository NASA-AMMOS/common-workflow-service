package jpl.cws.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
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

public class RestPostTask extends CwsTask {

	private static final String DEFAULT_BODY = null;
	private static final String DEFAULT_MEDIA_TYPE = null;
	private static final boolean DEFAULT_THROW_ON_BAD_RESPONSE = true;
	private static final boolean DEFAULT_ALLOW_INSECURE_REQUESTS = false;

	public static final String DEFAULT_CWS_COOKIE_NAME = CwsSecurityFilter.CWS_TOKEN_COOKIE_NAME;
	
	private static final String CWS_VARS_FROM_STDOUT_REGEX = "\\\"?cwsVariables\\\"?\\s*:\\s*(\\{(.+?)\\})";
	private static Pattern p = Pattern.compile("^"+CWS_VARS_FROM_STDOUT_REGEX+"$");


	private Expression url;
	private Expression body;
	private Expression contentType;
	private Expression throwOnBadResponse;
	private String urlString;
	private String bodyString;
	private String contentTypeString;
	private boolean throwOnBadResponseBoolean;
	private Expression allowInsecureRequests;
	private boolean allowInsecureRequestsBoolean;
	
	// CWS token security scheme variables
	//
	private Expression cwsTokenFileLocation;   // absolute path to CWS token file
	private String cwsTokenFileLocationString;
	private Expression cwsCookieName;       // cookie name that CWS expects
	private String cwsCookieNameString;
	private Expression cwsCookieValue;      // cookie value -- this is the CWS "token"
	private String cwsCookieValueString;
	

	public RestPostTask() {
		log.trace("RestPostTask constructor...");
	}

	@Override
	public void initParams() throws Exception {
		urlString = getStringParam(url, "url");
		allowInsecureRequestsBoolean = getBooleanParam(allowInsecureRequests, "allowInsecureRequests", DEFAULT_ALLOW_INSECURE_REQUESTS);
		bodyString = getStringParam(body, "body", DEFAULT_BODY);
		contentTypeString = getStringParam(contentType, "contentType", DEFAULT_MEDIA_TYPE);
		throwOnBadResponseBoolean = getBooleanParam(throwOnBadResponse, "throwOnBadResponse", DEFAULT_THROW_ON_BAD_RESPONSE);
		
		// CWS token security scheme variables
		//
		cwsTokenFileLocationString = getStringParam(cwsTokenFileLocation, "cwsTokenFileLocation", null);
		cwsCookieNameString = getStringParam(cwsCookieName, "cwsCookieName", DEFAULT_CWS_COOKIE_NAME);
		cwsCookieValueString = getStringParam(cwsCookieValue, "cwsCookieValue", null);
	}

	@Override
	public void executeTask() throws Exception {
		log.info("Starting REST POST operation (url = '" + urlString + "') ...");

		// if necessary variables are set,
		// then get the token to use as a cookie value
		//
		String cookieToUse = null;
		if (cwsTokenFileLocationString != null) {
			cwsCookieValueString = getCwsTokenFromFile();
			if (cwsCookieValueString != null) {
				cookieToUse = cwsCookieNameString + "=" + cwsCookieValueString;
			}
		}
		else {
			log.debug("not using CWS token approach -- and that's okay.");
		}

		// Make the REST POST
		//
		RestCallResult result = WebUtils.restCall(
				urlString, "POST",
				bodyString,
				cookieToUse,
				null,
				contentTypeString,
				allowInsecureRequestsBoolean);
		
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
		
		log.info("Finished REST POST operation.");
	}

	/**
	 * 
	 */
	private String getCwsTokenFromFile() throws Exception {
		BufferedReader br = null;
		try {
			File cwsTokenFile = new File(cwsTokenFileLocationString);
			if (!cwsTokenFile.exists()) {
				log.error("CWS token file does not exist at: " + cwsTokenFileLocationString);
				return null;
			}
			FileReader fr = new FileReader(cwsTokenFile);
			br = new BufferedReader(fr);
			return br.readLine(); // read and return the token
			
			// FIXME:  need to parse netscape cookie file format!!
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
		if (response == null) {
			log.error("response is null!");
			return;
		}
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
	
	public Expression getBody() {
		return body;
	}

	public void setBody(Expression body) {
		this.body = body;
	}

	public Expression getContentType() {
		return contentType;
	}

	public void setMediaType(Expression contentType) {
		this.contentType = contentType;
	}

	public Expression getThrowOnBadResponse() {
		return throwOnBadResponse;
	}

	public void setThrowOnBadResponse(Expression throwOnBadResponse) {
		this.throwOnBadResponse = throwOnBadResponse;
	}
}
