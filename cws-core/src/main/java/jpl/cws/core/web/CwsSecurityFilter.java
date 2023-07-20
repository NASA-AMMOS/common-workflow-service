package jpl.cws.core.web;

import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.FilterConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.impl.digest._apacheCommonsCodec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import jpl.cws.core.service.SecurityService;
import jpl.cws.core.service.SpringApplicationContext;
import jpl.cws.core.web.WebUtils.RestCallResult;

/**
 * Filter responsible for controlling access to CWS resources.
 * 
 * @author ghollins
 *
 */
public abstract class CwsSecurityFilter implements javax.servlet.Filter {
	private static final Logger log = LoggerFactory.getLogger(CwsSecurityFilter.class);

	public static final String CWS_TOKEN_COOKIE_NAME = "cwsToken";
	
	static final String COOKIES_HEADER = "Set-Cookie";
	
	protected String baseUrl;
	protected String contextPath;
	
	protected SecurityService cwsSecurityService;
	protected AuthorizationService authorizationService;

	protected String cwsSecurityScheme;
	
	public void init(FilterConfig filterConfig) {
		try {
			cwsSecurityScheme = filterConfig.getInitParameter("identityPluginType");
			log.debug("CWS Security scheme is: " + cwsSecurityScheme);

			this.contextPath = filterConfig.getServletContext().getContextPath();
			
			// This filter gets constructed (and initialized via this method)
			// for each web application under tomcat.
			// We only really want security to apply to the cws-ui webapp,
			// so only get service here.
			//
			if (contextPath.equals("/cws-ui") || contextPath.equals("/cws-engine")) {
				cwsSecurityService = (SecurityService) SpringApplicationContext.getBean("cwsSecurityService");
				authorizationService = (AuthorizationService) SpringApplicationContext.getBean("authorizationService");
				log.debug("got sec service: " + cwsSecurityService);
			}
		}
		catch (Exception e) {
			log.error("error during init", e);
			throw e;
		}
	}


//	@Override
//	public void afterPropertiesSet() throws Exception {
//		// TODO Auto-generated method stub
//		log.debug("afterPropertiesSet cwsSecurityService = " + cwsSecurityService);
//	}
//	
	
	abstract protected void logout(HttpServletRequest req, HttpServletResponse resp, HttpSession session);
	
	
	/**
	 * Looks for credentials (username and password) in the request,
	 * and returns them if found.
	 * 
	 * Credentials can be:
	 *  request parameters (i.e. from a login form)
	 *  OR
	 *  HTTP Basic Auth header value (encoded)
	 * 
	 */
	protected String[] getCredentialsFromRequest(HttpServletRequest req) {
		// Get the username and password from request, if they are there
		//
		String creds[] = {req.getParameter("username"), req.getParameter("password")};
		
		if (creds[0] == null || creds[1] == null) {
			// No form-provided username/password, so check to see
			// if any were provided via HTTP Basic Auth header
			//
			creds = getBasicAuthCredsFromRequest(req);
			if (creds == null) {
				if (log.isTraceEnabled()) {
					log.trace(req.getRequestURI() + " -- No credentials found in username and/or password from form and/or Basic Auth not specified.");
				}
				return null; // not authenticated
			}
			else {
				if (creds[0] == null || creds[1] == null) {
					log.warn("Only partial username/password found in Basic Auth header");
					return null; // not authenticated
				}
				// successfully obtained HTTP Basic Auth creds
				log.trace("Got HTTP Basic Auth provided credentials.");
			}
		}
		else {
			// successfully obtained form-provided creds
			log.trace("Got form-provided credentials.");
		}
		
		return creds;
	}
	
	
	/**
	 * Gets and returns HTTP Basic Auth credentials from request,
	 * if they are present.
	 * 
	 * @return credentials, or null if none present
	 */
	protected String[] getBasicAuthCredsFromRequest(HttpServletRequest req) {
		Enumeration<String> reqHeaderNames = req.getHeaderNames();
		while (reqHeaderNames.hasMoreElements()) {
			String reqHeaderName = reqHeaderNames.nextElement();
			//log.trace("HEADER: " +reqHeaderName +", VALUE: " + req.getHeader(reqHeaderName));
			if (reqHeaderName.equalsIgnoreCase("authorization")) {
				final String auth = req.getHeader(reqHeaderName);
				if (auth != null && auth.startsWith("Basic")) {
					// Authorization: Basic base64credentials
					String base64Credentials = auth.substring("Basic".length()).trim();
					String credentials =
						new String(Base64.decodeBase64(base64Credentials), Charset.forName("UTF-8"));
					// credentials = username:password
					final String[] values = credentials.split(":",2);
					// TODO: validate split was good
					return values;
				}
			}
		}
		return null; // nothing found
	}
	
	
	/**
	 * Posts a login to the Camunda web apps
	 * 
	 */
	protected HttpCookie postCamundaLogin(String baseServerUrl, String username, String password) throws Exception {
		HttpCookie retCookie = null;
		log.trace("Starting Camunda HTTP POST operation (username="+username+")...");
		//String urlString = "https://localhost:8443/camunda/api/admin/auth/user/default/login/cockpit";
		// TODO: should this always be the cockpit?
		
		
		
		String urlString = baseServerUrl + "/camunda/api/admin/auth/user/default/login/cockpit";
		
		int tries = 0;
		while (tries++ < 3) {
			HttpsURLConnection connection = null;
			try {
				log.debug("Attempting (#" + tries + ") POST to Camunda Web app: " + urlString);
				URL url = new URL(urlString);
				connection = (HttpsURLConnection) url.openConnection();
				HostnameVerifier origHostNameVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier();
				javax.net.ssl.HostnameVerifier looseHv = new javax.net.ssl.HostnameVerifier() {
					public boolean verify(String hostName,javax.net.ssl.SSLSession session) {
						return true;
					}
				};
				javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(looseHv);
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				connection.setRequestProperty("Accept", "application/json");
				OutputStream out = connection.getOutputStream();
				String urlEncodedUsername = URLEncoder.encode(username, "UTF-8");
				String urlEncodedPassword = URLEncoder.encode(password, "UTF-8");
				IOUtils.write("username=" + urlEncodedUsername + "&password=" + urlEncodedPassword, out);
			
				out.close();
				connection.connect();
				
				// set back
				javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(origHostNameVerifier);
				int responseCode = -1;
				try {
					responseCode = connection.getResponseCode();
					log.trace("got code = " + responseCode);
				} catch (Exception e) {
					log.warn("Camunda authentication via REST POST FAILED.  Response code was " + responseCode + ", message was " + connection.getResponseMessage(), e);
					responseCode = 404;
					break; // unrecoverable fail -- don't attempt again
				}
				
				if (responseCode == 200) {
					log.trace("Successfully authenticated user '" + username + "' with Camunda, via REST POST operation.");
					Map<String, List<String>> headerFields = connection.getHeaderFields();
					List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
					if (cookiesHeader != null) {
						for (String cookie : cookiesHeader) {
							retCookie = HttpCookie.parse(cookie).get(0);
							log.trace("SET COOKIE: " + retCookie);
						}
					}
					break; // success -- don't attempt again
				}
				else {
					log.warn("Camunda authentication via REST POST FAILED.  Response code was " + responseCode + ", message was " + connection.getResponseMessage());
					if (responseCode == 403 && urlString.endsWith("cockpit")) {
						// try using "tasklist" app to login instead..
						urlString = urlString.replace("cockpit", "tasklist");
						log.warn("trying again with: " + urlString);
						continue; // try again with modified URL
					}
					else {
						log.error("giving up on Camunda auth POST (" + urlString + ")");
						break; // give up trying
					}
				}
			}
			catch (Exception e) {
				log.error("Exception ", e);
			}
			finally {
				// cleanup
				try {
					connection.disconnect();
				} catch (Exception e) {
					// do nothing
				}
			}
		} // end while
		
		return retCookie;
	}
	
	
	// TODO: pass request as an argument instead?
	protected boolean isSecurityExemptResource(String path) {
		// TODO: optimize performance of this logic?  final static strings?
		// TODO: optimize by caching path and pass/fail in static Map
		log.trace("isSecurityExemptResource? path = " + path);
		
		//host.startsWith("0:0:0:0:0:0:0:1") ||
		
		if (path.endsWith(".js")  ||
				path.endsWith(".gif") ||
				path.endsWith(".css") ||
				path.endsWith(".png") ||
				path.endsWith("/favicon.ico")) {
			return true; // skip
		}
		if (path.startsWith("/camunda/")) {
			if (path.endsWith("login") ||
				path.endsWith("login/tasklist") ||
				path.endsWith("login/cockpit")) {
				return true; // skip
			}
		}
		else if (path.startsWith("/engine-rest/authorization") ) {
			return true; // skip
		}
		else if (path.startsWith("/cws-ui/") ) {
			if (path.startsWith("/cws-ui/fonts/") ||
				path.endsWith("login") ||
				path.endsWith("not_authorized") ||
				//path.toLowerCase().endsWith("/api/authenticate.mvc") ||
				path.toLowerCase().endsWith("/api/validatecwstoken.mvc") ||
				path.toLowerCase().contains("/api/checksession.mvc")
			) {
				return true; // skip
			}
		}
//		else if (path.startsWith("/cws-engine/api/") ) {
//			log.debug("cws-engine/api call.  SE");
//			return true; // skip
//		}
		else if (!path.startsWith("/cws-ui") && !path.startsWith("/cws-engine") && !path.startsWith("/camunda") && !path.startsWith("/engine-rest")) {
			return true;	// We do not want to implement security for other projects (e.g. project customizations).
		}
		else if (path.toLowerCase().endsWith("/logout")) {
			return true; // skip
		}
		
		return false;  // DON'T skip
	}

	// Simple override of http return for redirect code when http request is valid
	protected void statusOverride(HttpServletResponse resp){
		if (resp.getStatus() == 200){
			resp.setStatus(301);
		}
	}
	
	protected void logRequestInfo(HttpServletRequest req) {
		// Log all of the headers
		Enumeration<String> reqHeaderNames = req.getHeaderNames();
		while (reqHeaderNames.hasMoreElements()) {
			String reqHeaderName = reqHeaderNames.nextElement();
			log.debug("REQ HEADER: " + reqHeaderName + " : " + req.getHeader(reqHeaderName));
		}
		
		HttpSession session = req.getSession();
		
		// log all of the session attribute names
		Enumeration<String> attrNames = session.getAttributeNames();
		log.debug("session ID: " + session.getId());
		while (attrNames.hasMoreElements()) {
			String attrName = attrNames.nextElement();
			String attrValue = session.getAttribute(attrName).toString();
			log.debug("SESSION ATTR: "+ attrName + "==" + attrValue);
		}
	}
	
	
	protected String getBaseUrl(HttpServletRequest req) {
		if (baseUrl != null) { return baseUrl; } // return cached
		StringBuffer baseUrlBuf = new StringBuffer();
		baseUrlBuf.append(req.getScheme());
		baseUrlBuf.append("://");
		baseUrlBuf.append(req.getServerName());
		baseUrlBuf.append(":");
		baseUrlBuf.append(req.getServerPort());
		baseUrl = baseUrlBuf.toString(); // update cache
		return baseUrl;
	}
	
	
	/**
	 * Checks whether the specified CWS token is good (exists and not expired)
	 * 
	 */
	protected boolean cwsTokenGood(HttpServletRequest req, String cwsToken) {
		try {
			String path = req.getRequestURI();

			log.trace("cwsTokenGood :: cwsTokenSession=" + cwsToken + ", path=" + path);

			if (path.startsWith("/cws-ui/") || path.startsWith("/cws-engine/")) {
				String username = getUsernameFromReq(req);
				CwsToken token = cwsSecurityService.getCwsToken(cwsToken, username);
				if (token == null) {
					log.trace("NO CWS TOKEN '" + cwsToken + "' found!");
					return false; // no token found
				} else {
					log.trace("got CWS token: " + token);
				}

				if (token.isExpired()) {
					log.warn("CWS token '" + cwsToken + "' has expired!");
					// remove token from DB and in-memory cache, since it's no good
					cwsSecurityService.deleteCwsToken(cwsToken, username);
					return false; // expired, so token is not good
				}

				// re-set missing session attribute
				if (username == null) {
					req.getSession().setAttribute("userId", token.getUsername());
				}
				return true; // token found and not expired yet
			} else {
				log.trace("not checking cwsToken, since path is " + path);
				return true; // good (ignore all other paths for now)
			}
		}
		catch (Exception e) {
			log.error("Error in cwsTokenGood", e);
			return false; // error
		}
	}
	
	
	/**
	 * 
	 */
	protected void clearCwsTokensMapEntry(HttpServletRequest req) {
		String cwsToken = getCwsTokenFromRequest(req);
		String username = getUsernameFromReq(req);
		cwsSecurityService.clearCwsTokenMapEntry(cwsToken, username);
	}
	
	
	/**
	 * Extract the username from the specified HttpServletRequest
	 * 
	 */
	protected String getUsernameFromReq(HttpServletRequest req) {
		// First, try to get from in-memory session
		//
		HttpSession session = req.getSession();
		Object userIdReqAttr = session.getAttribute("userId");
		if (userIdReqAttr != null) {
			return userIdReqAttr.toString();
		}
		else {
			// If not found in session, fallback to getting from the request parameters
			//
			String[] creds = getCredentialsFromRequest(req);
			if (creds != null) {
				return creds[0]; // username
			}
		}
		// could not determine username
		return null;
	}
	
	
	/**
	 * Determines whether the User is authorized to access a resource.
	 * 
	 * @return true if authorized, false otherwise
	 * 
	 */
	protected boolean isAuthorized(HttpServletRequest req, String username, String cookie) throws Exception {
		String path = req.getRequestURI();
		
		// Determine if authorization should be skipped
		// (applies to a few select URIs)
		//
		if (path.startsWith("/cws-ui/")) {
			if (path.endsWith("/home") ||
				path.endsWith("/logintotarget") ||
				path.endsWith("/api/process-instance/status.mvc") ||
				path.endsWith("/cws-ui/api/authenticate.mvc")) {
				log.trace("NOT AUTHORIZING CWS PATH: " + path);
				return true; // no authorization
			}
		}
		else if (path.startsWith("/camunda/")) {
			// Camunda will manage authorizations of its own pages
			log.trace("NOT AUTHORIZING CAMUNDA PATH: " + path);
			return true; // no authorization
		}
		else if (path.startsWith("/engine-rest/")) {
			// No need to authorize an authorization call
			log.trace("NOT AUTHORIZING CAMUNDA PATH: " + path);
			return true; // no authorization
		}
		else if (path.equals("/")) {
			return true; // skip -- will get redirected anyways
		}
		
		// Get userId
		if (username == null) {
			username = getUsernameFromReq(req);
			if (username == null) {
				throw new Exception("userId not determined, Java session must be invalid!");
			}
		}
		
		log.trace("Authorizing user '" + username + "' for URI '" + path + "'...");
		
		// TODO: implement group-based authorization (issue #187)
		// Ex:  engine-rest/authorization?userIdIn=ghollins
		List<Authorization> auths = authorizationService.createAuthorizationQuery().userIdIn(username).list();

		// Check to see if User is authorized to resource
		for (Authorization auth : auths) {

			log.trace("AUTH: " + auth);

			// The "0" resourceType represents
			//if (uth.get("resourceType").toString().equals("0.0")) {
			if (auth.getResourceType() == 0) {

				//String resourceId = auth.get("resourceId").toString();
				String resourceId = auth.getResourceId();
				log.trace("RESOURCE ID = " + resourceId);
				if (resourceId.equals("*")) {
					// if *, allowing any resource under the multiple webapps
					resourceId = "/(cws-ui|cws-engine|camunda)/.*";
					log.trace("rewrite(0) to " + resourceId);
				}
				else {
					// Camunda-defined apps (managed by Camunda)
					if (resourceId.equals("cockpit") ||
						resourceId.equals("tasklist")) {
						continue; // skip over patterns we don't manage
					}

					if (resourceId.equalsIgnoreCase("deployments") ||
						resourceId.equalsIgnoreCase("processes") ||
						resourceId.equalsIgnoreCase("workers")   ||
						resourceId.equalsIgnoreCase("snippets")  ||
						resourceId.equalsIgnoreCase("logs")      ||
						resourceId.equalsIgnoreCase("history")      ||
						resourceId.equalsIgnoreCase("modeler")   ||
						resourceId.equalsIgnoreCase("initiators")) {
						resourceId = "/cws-ui/(api/|)" + resourceId+ ".*";
						log.trace("rewrite(1) to " + resourceId);
					}

					// Allow all processes to be scheduled
					else if (resourceId.equals("process/*")) {
						resourceId = "/cws-ui/process/.*";
						log.trace("rewrite(2) to " + resourceId);
					}

					// Allow a specific process(es) to be scheduled
					else if (resourceId.startsWith("process/")) {
						resourceId = "/cws-ui/" + resourceId + "/*";
						resourceId = resourceId.replace("*", ".*"); // make sure wildcard are correct for regex
						resourceId = resourceId.replace("..*", ".*"); // re-fix any that were correct to begin with
						log.trace("rewrite(3) to " + resourceId);
					}
				}

				// See if resource matches
				try {
					Pattern p = Pattern.compile("^" + resourceId+ "$");
					if (p.matcher(path).matches()) {
						log.trace("USER (" + username + ") IS AUTHORIZED FOR '" + path + "'.  (map " + resourceId + " MATCHED)");
						return true;
					}
				}
				catch (Exception e) {
					log.error("problem compiling pattern from '" + resourceId+ "'");
					// TODO: send email notice warning that bad pattern specified...
				}
			}
		}

		log.debug("USER NOT AUTHORIZED (no authorization patterns matched)");
		return false; // didn't find a match above
	}
	
	
	/**
	 * Sets the CWS_TOKEN_COOKIE_NAME cookie
	 * This signifies that the User is fully logged into both CWS/Camunda
	 * 
	 */
	protected void setCwsTokenCookie(HttpServletRequest req, HttpServletResponse resp) {
		String cwsToken = req.getSession().getId();
		WebUtils.addCookie(CWS_TOKEN_COOKIE_NAME, cwsToken, null, "/", resp);
		cwsSecurityService.addNewCwsTokenToDb(cwsToken, getUsernameFromReq(req));
		//addCwsSessionId(getUsernameFromReq(req), req.getSession().getId());
	}
	
	
	/**
	 * Removes the CWS_TOKEN_COOKIE_NAME cookie.
	 * 
	 */
	protected void removeCwsTokenCookie(HttpServletRequest req, HttpServletResponse resp) {
		WebUtils.unsetCookie(resp, CWS_TOKEN_COOKIE_NAME, null, "/");
		cwsSecurityService.deleteCwsToken(req.getSession().getId(), getUsernameFromReq(req));
	}
	
	
	/**
	 * Looks for a CWS token (in a CWS cookie) in the request,
	 * and returns the token if found.
	 * 
	 */
	protected static String getCwsTokenFromRequest(HttpServletRequest req) {
		Cookie cookie = getCwsCookieFromRequest(req);
		if (cookie != null) {
			return cookie.getValue();
		}
		return null;
	}
	
	protected static Cookie getCwsCookieFromRequest(HttpServletRequest req) {
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName() != null && cookie.getName().equals(CWS_TOKEN_COOKIE_NAME)) {
					log.trace("CWS TOKEN: " + cookie.getValue());
					return cookie;
				}
			}
		}
		return null;
	}
	
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		System.out.println("CwsSecurityFilter.destroy()...");
	}

}