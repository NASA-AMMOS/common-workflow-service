package jpl.cws.core.web;



import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**

 */
public class CwsSecurityValve extends ValveBase {
	//private static final Logger log = LoggerFactory.getLogger(CwsSecurityValve.class);
//	private SecurityService cwsSecurityService;
	
	public CwsSecurityValve() {
		System.out.println("CwsSecurityValve xtor");
	}
	
	/**
	 * 
	 */
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		System.out.println("IIIIIIIIIIIIIIIIIIIIIIIIIIIIII " + request.getRequestURI());
		
//		WebApplicationContext springContext = 
//				WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());

//		cwsSecurityService = (SecurityService) SpringApplicationContext.getBean("cwsSecurityService");
//		System.out.println("got sec service: " + cwsSecurityService);

		HttpServletRequest httpServletRequest = request.getRequest();

		Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

//		while (headerNames.hasMoreElements()) {
//			String header = headerNames.nextElement();
//			System.out.println("VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV Header --> " + header + ",  Value --> " + httpServletRequest.getHeader(header));
//		}
//		System.out.println("");

		getNext().invoke(request, response);
	}
}

