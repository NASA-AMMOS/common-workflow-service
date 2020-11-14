package jpl.cws.core.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Useful helper class from:
 * 
 * http://sujitpal.blogspot.com/2007/03/accessing-spring-beans-from-legacy-code.html
 * 
 * with additions and modifications by ghollins
 * 
 * Wrapper to always return a reference to the Spring Application Context from
 * within non-Spring enabled beans. Unlike Spring MVC's WebApplicationContextUtils
 * we do not need a reference to the Servlet context for this. All we need is
 * for this bean to be initialized during application startup.
 */
public class SpringApplicationContext implements ApplicationContextAware, BeanFactoryPostProcessor {
	private static final Logger log = LoggerFactory.getLogger(SpringApplicationContext.class);
	private static ApplicationContext CONTEXT;
	private ConfigurableListableBeanFactory factory;

	public SpringApplicationContext() {
		log.debug("SpringApplicationContext xtor");
	}
	
	/**
	 * This method is called from within the ApplicationContext once it is 
	 * done starting up, it will stick a reference to itself into this bean
	 * @param context a reference to the ApplicationContext.
	 */
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		CONTEXT = context;
	}
	
	
	public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
		this.factory = factory;
	}

	/**
	 * This is about the same as context.getBean("beanName"), except it has its
	 * own static handle to the Spring context, so calling this method statically
	 * will give access to the beans by name in the Spring application context.
	 * As in the context.getBean("beanName") call, the caller must cast to the
	 * appropriate target class. If the bean does not exist, then a Runtime error
	 * will be thrown.
	 * @param beanName the name of the bean to get.
	 * @return an Object reference to the named bean.
	 */
	public static Object getBean(String beanName) {
		log.trace("CWS: Getting bean '"+beanName+"' from context "+CONTEXT);
		return CONTEXT.getBean(beanName);
	}
	
	public static <T> Map<String,T> getBeansOfType(Class<T> type) {
		return CONTEXT.getBeansOfType(type);
	}
	
	
	public void replaceBean(String beanName, Class clazz) {
		BeanDefinitionRegistry registry = ((BeanDefinitionRegistry) factory);
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(clazz);
		beanDefinition.setLazyInit(false);
		beanDefinition.setAbstract(false);
		beanDefinition.setAutowireCandidate(true);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}
	
	public void replaceBean(String beanName, ConstructorArgumentValues constructorArgValues, MutablePropertyValues propVals, Class clazz) {
		BeanDefinitionRegistry registry = ((BeanDefinitionRegistry )factory);
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(clazz);
		beanDefinition.setLazyInit(false);
		beanDefinition.setAbstract(false);
		beanDefinition.setAutowireCandidate(true);
		if (constructorArgValues != null) {
			beanDefinition.setConstructorArgumentValues(constructorArgValues);
		}
		if (propVals != null) {
			beanDefinition.setPropertyValues(propVals);
		}
		try {
			//
			// NOTE:  THE BELOW CODE IS REPEATED TWICE.
			//        FOR SOME YET UNKNOWN REASON THIS IS NECESSARY
			//        FOR THE BEAN REGISTRATION TO FULLY WORK!!!
			//        If this is not done twice, then subsequent attempts
			//        to find the bean in the context yield nothing.
			//        If time permits, this should be investigated further..
			//
			if (registry.isBeanNameInUse(beanName)) {
				registry.removeBeanDefinition(beanName);
			}
			registry.registerBeanDefinition(beanName, beanDefinition);
			if (registry.isBeanNameInUse(beanName)) {
				registry.removeBeanDefinition(beanName);
			}
			registry.registerBeanDefinition(beanName, beanDefinition);
		}
		catch  (Exception e) {
			log.error("exception while replaceBean", e);
		}
		log.debug("replaced bean '" + beanName + "' (class is '" + clazz.getSimpleName() + "', CAVs=" + constructorArgValues + ", propVals=" + propVals + ")");
	}
	
	
	public <T> String[] getBeanDefinitionNamesOfType(Class<T> type) {
		return CONTEXT.getBeanNamesForType(type);
	}
	
	
	public void removeBean(String beanName) {
		BeanDefinitionRegistry registry = ((BeanDefinitionRegistry )factory);
		log.trace("removing bean definition: " + beanName);
		registry.removeBeanDefinition(beanName);
	}
	
	
	public <T> void removeAllBeansOfType(Class<T> type) {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) CONTEXT.getAutowireCapableBeanFactory();
		for(String beanName : CONTEXT.getBeanNamesForType(type)){
			log.debug("REMOVING BEAN (of type " + type + "): " + beanName);
			registry.removeBeanDefinition(beanName);
		}
	}

}