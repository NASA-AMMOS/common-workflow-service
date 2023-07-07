package jpl.cws.core.web;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.core.web.WebUtils.RestCallResult;

/**
 * Security filter specific to the LDAP authentication scheme
 *
 * @author ghollins
 *
 */
public class CwsLdapSecurityFilter extends CwsSecurityFilter {
	private static final Logger log = LoggerFactory.getLogger(CwsLdapSecurityFilter.class);

	/*
	 * all resources protected behind filter.
	 *
	 * (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(
			ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

		HttpServletRequest req   = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;

		String path = req.getRequestURI();
		HttpSession session = req.getSession();

		if (log.isTraceEnabled()) {
			log.trace("PATH = " + path);
			Enumeration<String> reqHeaderNames = req.getHeaderNames();
			while (reqHeaderNames.hasMoreElements()) {
				String reqHeaderName = reqHeaderNames.nextElement();
				log.trace("REQ HEADER = " + reqHeaderName);
			}
		}

		if (path.toLowerCase().endsWith("/logout")) {
			logout(req, resp, session);
			chain.doFilter(request, resp); // continue onwards with chain
			return;
		}

		statusOverride(resp);

		// If skipping resource...
		//
		if (isSecurityExemptResource(path)) {
			log.trace("skipping PATH " + path);

			// Special case of redirecting requests to login page
			// to home page, if the User is already authenticated
			// TODO: do we want this block?
			if (path.toLowerCase().endsWith("/login")) {
				if (authenticate(req)) { // if authenticated
					resp.sendRedirect("/cws-ui/home");
				}
			}

			// FIXME:  add similar logic as above to redirect Camunda login pages..

			chain.doFilter(request, resp); // continue onwards with chain
			return;
		}
		else {
			log.trace(contextPath + ": Applying filter to path: " + path);
		}

		//
		// Attempt authentication
		//
		if (authenticate(req)) {
			log.trace("req: " + req + " IS AUTHENTICATED");

			// ========================
			// WE ARE AUTHENTICATED NOW
			// ========================

			// If we don't already have a CWS token cookie,
			// post login to Camunda web page, to obtain Camunda cookie.
			// This will set the Camunda cookie back to the response,
			// so Camunda web app functionality will work.
			//
			if (getCwsTokenFromRequest(req) == null) {
				if (postCamundaLoginIfNecessary(req, resp)) {
					// Set the CWS token cookie as the final step to
					// indicate authentication was successful for BOTH CWS and Camunda.
					// NOTE: We only want to do this if we are making a request for a CWS resource
					// (and therefore cwsSecurityService is not null).  If we are making a direct
					// request to Camunda, for example an engine-rest call, we don't set the CWS
					// token cookie.
					//
					if (cwsSecurityService != null) {
						setCwsTokenCookie(req, resp);
					}
				}
				else {
					log.warn("Posting Camunda login NOT successful.  Returning User to login page...");
					if (contextPath.equals("/cws-ui")) {
						logout(req, resp, session);
					}
					// Redirect User back to login page
					resp.sendRedirect("/cws-ui/login");
					return; //break filter chain, requested resource will not be accessed
				}
			}

			// Now authorize the user
			//
			try {
				if (!isAuthorized(req, null, CWS_TOKEN_COOKIE_NAME + "=" + req.getSession().getId())) {
					log.warn("User not authorized.  Redirecting to not authorized page...");
					// Redirect User back to login page
					resp.sendRedirect("/cws-ui/not_authorized");
					return; //break filter chain, requested resource will not be accessed
				}

			} catch (Exception e) {
				log.error("Problem authorizing user.  Invalidating session..", e);

				if (contextPath.equals("/cws-ui")) {
					logout(req, resp, session);
				}
				// Redirect User back to login page
				resp.sendRedirect("/cws-ui/login");
				return; //break filter chain, requested resource will not be accessed
			}
		}
		else {
			// =================
			// NOT AUTHENTICATED
			// =================

			// For unauthenticated access attempts to the the cws-ui resource,
			// logout and redirect to the login page. Everything else should get a 403.
			//
			if (contextPath.equals("/cws-ui")) {
				logout(req, resp, session);
				resp.sendRedirect("/cws-ui/login");
			}
			else {
				// Redirect to 403 forbidden access page
				resp.sendError(403, "User must authenticate before accessing resource " + contextPath);
				log.debug("NOT AUTHENTICATED. Breaking filter chain with HTTP 403...");
			}

			return; //break filter chain, requested resource will not be accessed
		}

		// continue onwards with chain
		chain.doFilter(req, resp);
	}


	@Override
	protected void logout(HttpServletRequest req, HttpServletResponse resp, HttpSession session) {
		log.debug("Logout actions:  removing session state, invalidating session, removing CWS token cookie...");
		clearCwsTokensMapEntry(req);
		removeCwsTokenCookie(req, resp);
		session.invalidate();
	}


	/**
	 * Determines whether or not User is authenticated.
	 *
	 * @return true if authentication is a success, false otherwise
	 */
	private boolean authenticate(HttpServletRequest req) {
		String baseUrl = getBaseUrl(req);
		try {
			// First, check CWS token to see if we are already authenticated
			//
			String cwsToken = getCwsTokenFromRequest(req);
			log.trace("cwsToken = " + cwsToken);
			log.trace("session  = " +req.getSession().getId());
			if (cwsToken != null) {
				if (cwsTokenGood(req, cwsToken)) {
					return true; // CWS token IS good
				}
				else {
					//
					// CWS token NOT good
					//
					if (getUsernameFromReq(req) != null) {
						// Session has expired that corresponds to token.
						// We want to short-circuit here to force logout actions
						log.debug("session ("+baseUrl+") " + cwsToken + " has expired");
					}
					return false; // session token expired or User has not logged in yet
				}
			}


			// Next, attempt to authenticate using credentials
			//
			log.trace("not authenticated with token, so next trying to authenticate via credentials in request...");
			String[] credentials = getCredentialsFromRequest(req);
			if (credentials == null || credentials[0] == null || credentials[1] == null) {
				return false; // no credentials, so give up -- NOT authenticated
			}
			String username = credentials[0];
			String password = credentials[1];
			log.trace("username = " + username);

			if (!contextPath.equals("/cws-ui")) {
				log.debug("Authenticating with VIA cws-ui... " + username + " for " + baseUrl);

				RestCallResult result = WebUtils.restCall(
						baseUrl + "/cws-ui/rest/authenticate",
						"POST",
						"username=" + username + "&password=" + password);

				return result.getResponseCode() == 200;  // Success! We are authenticated
			}
			else {
				log.debug("Authenticating with " + username + " for " + baseUrl);
				return cwsSecurityService.checkCredentialsViaIdentityService(username, password);
			}

		} catch (Exception e) {
			log.error("exception when authenticate called", e);
		}

		return false;
	}


	/**
	 * Determines whether it's necessary to post to Camunda web app login,
	 * and makes the post if it's necessary.
	 *
	 */
	private boolean postCamundaLoginIfNecessary(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// FIXME: Only do this if we are making a non-automated call? (i.e. we are using web apps as a User)
		//
		boolean isHuman = true; // hard-coded until fixed
		if (isHuman) {
			try {
				log.trace("We don't already have the Camunda token, so posting login to Camunda web app to obtain...");

				// Next, attempt to authenticate using credentials next
				//
				String[] credentials = getCredentialsFromRequest(req);
				if (credentials == null || credentials[0] == null || credentials[1] == null) {
					log.error("we don't have Camunda logged in cookie, and we also don't have credentials. " +
							"Not posting to Camunda login page!");
					return false;
				}
				String username = credentials[0];
				String password = credentials[1];
				String baseUrl = getBaseUrl(req);
				log.trace("about to post to Camunda with " + username +" (baseUrl="+baseUrl+")");
				HttpCookie camundaCookie = postCamundaLogin(baseUrl, username, password);
				if (camundaCookie != null) {
					// Set Camunda session cookie
					Cookie cookieToSet = WebUtils.constructCookie(camundaCookie.getName(), camundaCookie.getValue(), null, camundaCookie.getPath());
					log.trace("Setting Camunda session cookie: " + cookieToSet.getName() + "-->" + cookieToSet.getValue());
					resp.addCookie(cookieToSet);
					req.getSession().setAttribute("userId", username);
					return true;
				}
				else {
					return false;
				}
			} catch (Exception e) {
				log.error("Exception on posting Camunda login form", e);
				resp.sendRedirect("/cws-ui/login");
				return false;
			}
		}
		return true;
	}

}
