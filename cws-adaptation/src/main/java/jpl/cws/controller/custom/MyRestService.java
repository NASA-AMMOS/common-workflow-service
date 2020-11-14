package jpl.cws.controller.custom;


import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api")
public class MyRestService {
	private static final Logger log = LoggerFactory.getLogger(MyRestService.class);


	public MyRestService() {
		System.out.println("MyRestService xtor");
	}
	
	

	/**
	 * Example POST service
	 * 
	 */
	@RequestMapping(value = "/example", method = POST)
	public @ResponseBody String example(
			final HttpSession session) {
		
		log.info("*** REST CALL ***  example");
		
		return "success";
	}
	
}