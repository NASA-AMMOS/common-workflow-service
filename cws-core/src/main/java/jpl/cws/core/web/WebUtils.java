package jpl.cws.core.web;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.TrustManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

public class WebUtils {
	private static final Logger log = LoggerFactory.getLogger(WebUtils.class);
	
	private static final String TSPECIALS = "/()<>@,;:\\\"[]?={} \t";
	
	public static class RestCallResult {
		private int responseCode;
		private String response;
		private String responseMessage;
		
		public RestCallResult (int responseCode, String response, String responseMessage) {
			this.responseCode = responseCode;
			this.response = response;
			this.responseMessage = responseMessage;
		}
		
		public int getResponseCode() { return responseCode; }
		public String getResponse() { return response; }
		public String getResponseMessage() { return responseMessage; }
	}
	
	/**
	 * Performs a REST call
	 *   (defaults to no cookie, default Content-Type, and default allow only valid certificates)
	 */
	public static RestCallResult restCall(String urlString, String method, String postData) throws Exception {
		return restCall(urlString, method, postData,
			null,  // cookie
			null,  // acceptType
			null  // contentType
		);
	}

	/**
	 * Performs an unauthenticated REST call
	 */
	public static RestCallResult restCall(String urlString, String method, String data, String cookie, String acceptType, String contentType) throws Exception {
		return restCall(urlString, method, data, cookie, acceptType, contentType, urlString.startsWith("https") ? false : true, null, null);
	}

	/**
	 * Performs an authenticated REST call
	 */
	public static RestCallResult restCall(String urlString, String method, String data, String cookie, String acceptType, String contentType, String username, String password) throws Exception {
		return restCall(urlString, method, data, cookie, acceptType, contentType, false, username, password);
	}
	
	/**
	 * Performs a REST call
	 * 
	 */
	public static RestCallResult restCall(String urlString, String method, String data, String cookie, String acceptType, String contentType, Boolean allowInsecureRequests, String username, String password) throws Exception {
		log.debug("urlString = " + urlString);
		HttpURLConnection connection = null;
		try {
			
			String[] rootAndQueryString = urlString.split("\\?");
			if (rootAndQueryString.length == 2) {
				String encodedQs = UriUtils.encodeQuery(rootAndQueryString[1], "UTF-8");
				urlString = rootAndQueryString[0] + "?" + encodedQs;
			}
			else if (rootAndQueryString.length > 2) {
				log.error("unexpected urlString: '" + urlString + "'!");
			}
			
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();

			// Add authentication to request
			if (username != null) {
				String userpass = username + ":" + password;
				String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
				connection.setRequestProperty ("Authorization", basicAuth);
			}

			if (allowInsecureRequests) {

				log.info("SSL 'insecure' mode activated.");

				if (connection instanceof HttpsURLConnection) {
					HttpsURLConnection httpsConnection = (HttpsURLConnection)connection;

					TrustAllCertificates trustAll = new TrustAllCertificates();

					// Install the all-trusting trust manager
					SSLContext sc = SSLContext.getInstance("SSL");
					sc.init(null, 
							new TrustManager[]{ trustAll },
							new java.security.SecureRandom());

					httpsConnection.setHostnameVerifier(trustAll);
					httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
				}
			}
			
			// User must opt-in to insecure requests by setting allowInsecureRequests = true.
			// If that flag is not set, then don't allow insecure connections.
			//
			if (!allowInsecureRequests && !(connection instanceof HttpsURLConnection)) {
				log.error("Attempted to make a non SSL connection, without the allowInsecureReuquests flag set. "
						+ "Not performing this request!");
				return null; // abort
			}
			
			log.trace("method = " + method + ", url = " + urlString+", data = "+ data);
			
			connection.setRequestMethod(method);
			
			// set cookie if provided
			if (cookie != null) {
				log.trace("making REST " + method + " call with cookie: " + cookie);
				connection.addRequestProperty("Cookie", cookie);
			}
			
			// set "Accept" request header, if provided
			if (acceptType != null) {
				connection.addRequestProperty("Accept", acceptType);
			}
			
			// set content type if provided
			if (contentType != null) {
				connection.addRequestProperty("Content-Type", contentType);
			}
			else {
				// default to a Content-Type of "text/plain"
				connection.addRequestProperty("Content-Type", "text/plain");
			}
			
			connection.setDoOutput(true);
			
			// Send POST args, if specified
			if (method.equals("POST") || method.equals("PUT")) {
				if (data != null) {
					OutputStream out = connection.getOutputStream();
					IOUtils.write(data, out);
					out.close();
				}
			}

			// Perform the connection
			connection.connect();

			int responseCode = -1;
			try {
				responseCode = connection.getResponseCode();
			} catch (Throwable e) {
				log.warn("Exception while get response code", e);
				responseCode = 404;
			}
			if (responseCode == 200) {
				log.trace("REST "+method+" for " + url + " was successful (HTTP 200).");

				InputStream in = connection.getInputStream();
				// FIXME: potential memory suck if response is large
				String response = IOUtils.toString(in);
				in.close();
				return new RestCallResult(responseCode, response, connection.getResponseMessage());
			}
			else {
				log.warn("REST " + method + " FAILED for " + url + ".  Response code was " + responseCode);

				return new RestCallResult(responseCode, null, connection.getResponseMessage());
			}
		}
		catch (SSLProtocolException e) {
			log.error("SSL certificate problem.  Try setting 'Insecure Requests' to \"true\" in CWS Modeler to allow connections to SSL sites without certificates.");
			
			throw new Exception("restCall (" + urlString + ") -- FAILED", e);
		}
		catch (Exception e) {
			throw new Exception("restCall (" + urlString + ") -- FAILED", e);
		}
		finally {
			// cleanup
			try {
				connection.disconnect();
			} catch (Exception e) {
				log.warn("Exception on disconnect", e);
			}
		}
	}


	/**
	 * 
	 */
	public static void addCookie(String name, String value, String domain, String path, HttpServletResponse resp) {
		if (!isValidCookieString(name) || !isValidCookieString(value)) {
			throw new IllegalArgumentException("Cookie name and/or value is invalid (contains unacceptable characters)!");
		}
		Cookie cookie = constructCookie(name, value, domain, path);
		resp.addCookie(cookie);
	}

	public static void addUnsecureCookie(String name, String value, String domain, String path, HttpServletResponse resp) {
		if (!isValidCookieString(name) || !isValidCookieString(value)) {
			throw new IllegalArgumentException("Cookie name and/or value is invalid (contains unacceptable characters)!");
		}
		Cookie cookie = constructCookie(name, value, domain, path);
		cookie.setHttpOnly(false);
		resp.addCookie(cookie);
	}
	

	
	/**
	 * Unsets a cookie in the specified response.
	 * 
	 */
	public static void unsetCookie(HttpServletResponse resp, String cookieName, String domain, String path) {
		Cookie cookie = null; 
		try {
			cookie = constructCookie(cookieName, null, domain, path);
		}
		catch (Throwable t) {
			log.error("Unable to construct cookie, so cookie will not be unset!");
			return;
		}
		cookie.setMaxAge(0); // Don't set to -1 or it will become a session cookie!
		resp.addCookie(cookie);
	}
	
	
	/**
	 * Constructs a safe/secure cookie.
	 * 
	 */
	public static Cookie constructCookie(String name, String value, String domain, String path) {
		if (!isValidCookieString(name) ||
			(value != null && !isValidCookieString(value))) {
			throw new IllegalArgumentException("Cookie name and/or value is invalid (contains unacceptable characters)!");
		}
		Cookie cookie = new Cookie(name, value);
		if (domain != null) { cookie.setDomain(domain); }
		if (path != null)   { cookie.setPath(path); }
		log.trace("Setting '" + name + "' cookie to " + value + " (domain=" + domain + ", path=" + path + ")");
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		return cookie;
	}
	
	
	/*
	 * Tests a string and returns true if the string counts as a 
	 * reserved token in the Java language.
	 */
	private static boolean isValidCookieString(String value) {
		int len = value.length();
		for (int i = 0; i < len; i++) {
			char c = value.charAt(i);
			if (c < 0x20 || c >= 0x7f || TSPECIALS.indexOf(c) != -1) {
				return false;
			}
		}
		return true;
	}
	
}