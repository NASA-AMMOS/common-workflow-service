package jpl.cws.controller;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jpl.cws.process.initiation.CwsProcessInitiator;
import jpl.cws.process.initiation.InitiatorsService;
import jpl.cws.scheduler.Scheduler;
import jpl.cws.service.CwsConsoleService;

@Controller
public class MvcService extends MvcCore {
	private static final Logger log = LoggerFactory.getLogger(MvcService.class);

	@Autowired private RepositoryService repositoryService;
	@Autowired private IdentityService identityService;
	@Autowired private AuthorizationService authorizationService;
	@Autowired private Scheduler scheduler;
	@Autowired private CwsConsoleService cwsConsoleService;
	@Autowired private InitiatorsService cwsInitiatorsService;
	
	@Value("${cws.console.app.root}") private String appRoot;

	public MvcService() {}

	/**
	 *
	 */
	@RequestMapping(value = "/login", method = GET)
	public ModelAndView login(final HttpSession session) {
		return buildModel("login", "Please log in");
	}
	
	
	/**
	 *
	 */
	@RequestMapping(value = "/not_authorized", method = GET)
	public ModelAndView notAuthorized(final HttpSession session) {
		return buildModel("not_authorized", "Not authorized. Please navigate elsewhere");
	}
	
	
	/**
	 * 
	 */
	@RequestMapping(value = "/home", method = GET)
	public ModelAndView index(final HttpSession session) {
		return buildHomeModel("");
	}

	/**
	 *
	 */
	@RequestMapping(value = "/summary", method = GET)
	public ModelAndView summary(final HttpSession session) {
		return buildSummaryModel("");
	}
	
	/**
	 * Target URI of login form,
	 * causing authentication along the way (web filter),
	 * and results in displaying the home page with a welcome message
	 * 
	 */
	@RequestMapping(value = "/logintotarget", method = POST)
	public ModelAndView logintotarget(
			final HttpSession session,
			HttpServletResponse response,
			@RequestParam String username,
			@RequestParam String targetPage) {
		log.debug("passing through to home page (login attempt considered a success)... (username="+username+")");
		
		User user = identityService
				.createUserQuery()
				.userId(username)
				.singleResult();
		log.debug("user: " + user);
		
		String pattern =                            // first part: some/relative/path
			  "^"                                   // Anchor at the beginning of the string
			+ "(?!\\/)"                             // Assert the first character isn't a '/' (negative lookahead)
			+ "(?!.*\\/\\/)"                        // Assert there are no "//" present anywhere (negative lookahead)
			+ "[A-Za-z0-9-\\/]+"                    // Match one or more allowed characters
			+ "(?<!\\/)"                            // Assert the last character isn't a '/'
			                                        // second part: ?key=value
			+ "(\\?"                                // Match '?' (start capture group 1)
			+   "[A-Za-z0-9-]+=[A-Za-z0-9-]+"       // Match key=value format one or more times
			+     "(&[A-Za-z0-9-]+=[A-Za-z0-9-]+)*" // Followed by &key=value zero or more times
			+   "){0,1}"                            // Match capture group 1 zero or 1 times
			+ "$";                                  // Anchor at the end of the string

		// if target page is not null, then redirect to target page
		//
		if (targetPage != null) {

			// input validation for targetPage - make sure only relative paths with query params are allowed
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(targetPage);

			if (!m.find()) {
				// inavalid redirect, redirect to home instead
				log.warn("invalid redirect target at login (targetPage = " + targetPage + "), routing to home instead. was this a phishing attempt?");
				return buildHomeModel("Welcome " + (user == null ? username : user.getFirstName()));
			}

			try {
				log.debug("redirecting successful login (of user '" + username + "') to " + targetPage);
				response.sendRedirect(targetPage);
			} catch (IOException e) {
				log.error("IOException on redirect", e);
			}
			return null; // should not get here
		}
		else { // otherwise, go to CWS home page by default
			return buildHomeModel("Welcome " + (user == null ? username : user.getFirstName()));
		}
	}
	
	
	@RequestMapping(value = "/deployments", method = GET)
	public ModelAndView deployments() {
		return buildDeploymentsModel("");
	}
	
	
	@RequestMapping(value = "/logs", method = GET)
	public ModelAndView logs() {
		return buildLogsModel("");
	}
	
	
	@RequestMapping(value = "/history", method = GET)
	public ModelAndView history() {
		return buildHistoryModel("");
	}

	
	@RequestMapping(value = "/workers", method = GET)
	public ModelAndView workers() {
		return buildWorkersModel();
	}
	
	
	/**
	 * Returns Initiators page model and view
	 * 
	 */
	@RequestMapping(value = "/initiators", method = GET)
	public ModelAndView initiators() {
		ModelAndView model = new ModelAndView("initiators");
		model.addObject("base", appRoot);
		model.addObject("msg", "");
		try {
			Map<String,CwsProcessInitiator> initiatorsMap = cwsInitiatorsService.getProcessInitiators();
			List<CwsProcessInitiator> initiators = new ArrayList<CwsProcessInitiator>();
			for (Entry<String,CwsProcessInitiator> entry : initiatorsMap.entrySet()) {
				initiators.add(entry.getValue());
			}
			model.addObject("initiators", initiators);
		} catch (Exception e) {
			log.error("There was a problem getting listing of initiators", e);
		}
		return model;
	}
	
	
	@RequestMapping(value = "/snippets", method = GET)
	public ModelAndView snippets() {
		ModelAndView model = new ModelAndView("snippets");
		model.addObject("base", appRoot);
		model.addObject("msg", "");
		return model;
	}
	
	
	@RequestMapping(value = "/processes", method = GET)
	public ModelAndView processes() {
		return buildProcessesModel("");
	}
	
	
	@RequestMapping(value = "/configuration", method = GET)
	public ModelAndView configuration() {
		return buildConfigurationModel("");
	}
	
	
	@RequestMapping(value = "/documentation", method = GET)
	public ModelAndView documentation() {
		return buildDocumentationModel("");
	}
	
	
	@RequestMapping(value = "/modeler", method = GET)
	public ModelAndView modeler() {
		return buildModelerModel("");
	}
	
	
	/**
	 * 
	 */
	@RequestMapping(value = "/logout", method = GET)
	public ModelAndView logout(
			final HttpSession session,
			HttpServletRequest request,
			HttpServletResponse response) {
		log.info("User should now be logged out (CwsSecurityFilter should have already logged out User).");
		
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		
		try {
			response.sendRedirect("login");
		} catch (IOException e) {
			log.error("IOException on redirect", e);
		}
		return null; // should not get here
	}
}

