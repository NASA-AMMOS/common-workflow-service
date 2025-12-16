package jpl.cws.scheduler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class SchedulerQueueUtils {

	private static final Logger log = LoggerFactory.getLogger(SchedulerQueueUtils.class);
	
	// TODO: make these values come from configuration
	
	@Value("${cws.broker.obj.name:org.apache.activemq.artemis:broker=cwsConsoleBroker}") private String BROKER_OBJ_NAME;
	@Value("${cws.amq.jmx.service.url}") private String AMQ_JMX_SERVICE_URL;
	
	private static JMXServiceURL url;
	private static JMXConnector connector;
	private static MBeanServerConnection connection;
	private static ObjectName activeMQ;
	
	/**
	 * Logs queue status
	 * 
	 */
	public void logSchedulerQueues() {
		try {
			System.out.println("------------------------ SCHEDULER QUEUES -------------------------------");
			ActiveMQServerControl serverControl = getActiveMQServerControl();
			String[] queueNames = serverControl.getQueueNames();
			
			for (String queueName : queueNames) {
				QueueControl queueControl = getQueueControl(queueName);
				System.out.println("  "+queueControl.getName() + 
						" : [enqueues: " + queueControl.getMessageCount() +
						", dequeues: " + (queueControl.getMessageCount() - queueControl.getDeliveringCount()) +
						", inFlights: " + queueControl.getDeliveringCount()+"]");
			}
			System.out.println("------------------------------------------------------------------------");
		} catch (Exception e) {
			log.error("failed to log scheduler queue", e);
		}
	}
	
	
	/**
	 * 
	 */
	public Set<org.apache.activemq.artemis.core.server.ActiveMQServer> getAmqClients() throws Exception {
		Set<org.apache.activemq.artemis.core.server.ActiveMQServer> uniqueServers = new HashSet<>();
		
		// For Artemis, we'll use JMX to get server information instead
		// The embedded server instance is not easily accessible from this context
		try {
			ActiveMQServerControl serverControl = getActiveMQServerControl();
			log.trace("ARTEMIS SERVER CONTROL: " + serverControl);
			log.trace("  version: " + serverControl.getVersion());
			log.trace("  nodeID: " + serverControl.getNodeID());
			// Note: We can't get the actual server instance, but we have control access
		} catch (Exception e) {
			log.debug("Could not get ActiveMQ server control: " + e.getMessage());
		}
		
		return uniqueServers;
	}
	
	
	/**
	 * Returns true if the specified queue exists,
	 * false otherwise.
	 * 
	 */
	@Deprecated
	public boolean queueExists(String queueName) throws Exception {
		if (queueName == null || queueName.isEmpty()) {
			throw new IllegalAccessException("queueName was null or empty!");
		}
		log.debug("CHECKING FOR EXISTENCE OF SCHEDULER QUEUE: '"+queueName+"' ...");
		
		ActiveMQServerControl serverControl = getActiveMQServerControl();
		String[] queueNames = serverControl.getQueueNames();
		
		for (String existingQueueName : queueNames) {
			if (existingQueueName.equals(queueName)) {
				log.debug("SCHEDULER QUEUE: '"+queueName+"' EXISTS!");
				return true;
			}
		}
		log.debug("SCHEDULER QUEUE: '"+queueName+"' DOES NOT EXISTS.");
		return false;
	}
	
	
	/**
	 * 
	 */
	@Deprecated
	public void addQueue(String queueName) throws Exception {
		if (queueName == null || queueName.isEmpty()) {
			throw new IllegalAccessException("queueName was null or empty!");
		}
		log.debug("CREATING SCHEDULER QUEUE '"+queueName+"' ...");
		
		ActiveMQServerControl serverControl = getActiveMQServerControl();
		serverControl.createQueue(queueName, queueName, true, "ANYCAST");
	}
	
	
	/**
	 * 
	 */
	private MBeanServerConnection getConn() throws Exception {
		if (connection != null) {
			return connection;
		}
		else {
			if (url == null) {
				log.info("USING JMX URL: " + AMQ_JMX_SERVICE_URL);
				url = new JMXServiceURL(AMQ_JMX_SERVICE_URL);
			}
			if (connector ==  null) {
				connector = JMXConnectorFactory.connect(url, null);
			}
			connector.connect();
			connection = connector.getMBeanServerConnection();
		}
		return connection;
	}
	
	
	/**
	 * 
	 */
	private ObjectName getActiveMQ() throws MalformedObjectNameException {
		if (activeMQ != null) {
			return activeMQ;
		}
		activeMQ = new ObjectName(BROKER_OBJ_NAME);
		return activeMQ;
	}
	
	
	/**
	 *
	 */
	private ActiveMQServerControl getActiveMQServerControl() throws Exception {
		return (ActiveMQServerControl) MBeanServerInvocationHandler.newProxyInstance(
				getConn(), getActiveMQ(), ActiveMQServerControl.class, true);
	}
	
	/**
	 * 
	 */
	private QueueControl getQueueControl(String queueName) throws Exception {
		ObjectName queueObjectName = new ObjectName(ResourceNames.QUEUE + queueName);
		return (QueueControl) MBeanServerInvocationHandler.newProxyInstance(
				getConn(), queueObjectName, QueueControl.class, true);
	}
	
}
