package jpl.cws.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.cws.core.service.SpringApplicationContext;

public class ProcDefUtils {

	private static final Logger log = LoggerFactory.getLogger(ProcDefUtils.class);
	
	private Object cwsBean;
	
	public ProcDefUtils() {
		cwsBean = SpringApplicationContext.getBean("cws");
		log.trace("CWS Bean = " + cwsBean);
	}
	
	
	public String getClasspathUrls() {
		ClassLoader thisCl = this.getClass().getClassLoader();
		System.out.println("CL = " + thisCl);
		URL[] urls = ((URLClassLoader)thisCl).getURLs();
		for(URL url: urls){
			System.out.println("THIS CL: " + url.getFile());
		}
		
		ClassLoader threadCl = Thread.currentThread().getContextClassLoader();
		System.out.println("CL = " + threadCl);
		urls = ((URLClassLoader)threadCl).getURLs();
		for(URL url: urls){
			System.out.println("THREAD CL: " + url.getFile());
		}
		
		ClassLoader sysCl = ClassLoader.getSystemClassLoader();
		System.out.println("CL = " + sysCl);
		urls = ((URLClassLoader)sysCl).getURLs();
		String ret = "";
		for(URL url: urls){
			System.out.println("SYS CL : " + url.getFile());
		}
		
		return ret;
	}
	
	/**
	 * Invokes a Snippets (jpl.cws.core.code.Snippets) method, and returns the result.
	 * 
	 */
	public Object callSnippet(String methodName, Object ... params) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		log.debug("about to call method : " + methodName);
		Class<?>[] methodParamTypes = new Class<?>[params.length];
		int i = 0;
		for (Object param : params) {
			methodParamTypes[i] = param.getClass();
		}
		Method method = cwsBean.getClass().getDeclaredMethod(methodName, methodParamTypes);
		log.debug("got method: " + method);
		return method.invoke(cwsBean, params);
	}
	
}
