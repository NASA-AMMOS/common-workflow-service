package jpl.cws.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RequestMapping("/api")
public class EngineRestService {
	private static final Logger log = LoggerFactory.getLogger(EngineRestService.class);

	@Value("${cws.console.app.root}") private String appRoot;
	@Value("${cws.install.hostname}") private String hostName;

	public EngineRestService() {}
	

	/**
	 * Authenticates the User, as this passes through CWS security.
	 * Depending on the security scheme CWS is using,
	 * a cookie may be set in the response.
	 * 
	 * This cookie, can then be used to make future requests.
	 * 
	 */
	@RequestMapping(value="/authenticate", method = GET)
	public @ResponseBody String authenticateViaGet(
		final HttpSession session) {
		log.debug("/authenticate call got through CWS security!");
		return "{\"status\" : \"SUCCESS\", \"session\" : \"" + session.getId() + "\"}";
	}
	
	
	/**
	 * Authenticates the User, as this passes through CWS security.
	 * Depending on the security scheme CWS is using,
	 * a cookie may be set in the response.
	 * 
	 * This cookie, can then be used to make future requests.
	 * 
	 */
	@RequestMapping(value = "/authenticate", method = POST)
	public @ResponseBody String authenticateViaPost(
			final HttpSession session,
			HttpServletResponse response) {
		log.debug("/authenticate call got through CWS security!");
		return "{\"status\" : \"SUCCESS\", \"session\" : \"" + session.getId() + "\"}";
	}
	
	
	/**
	 * Validates CWS token (checks for expiration)
	 * 
	 */
	@RequestMapping(value = "/validateCwsToken", method = POST)
	public @ResponseBody String validateCwsToken(
			final HttpSession session,
			HttpServletResponse response,
			@RequestParam String cwsToken) {
		log.trace("validateCwsToken... (cwsToken="+cwsToken+", session.id="+session.getId()+")");
		boolean isValid = session.getId().equals(cwsToken);
		log.trace("/validateCwsToken returning " + isValid);
		if (!isValid) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
		return isValid+"";
	}

}