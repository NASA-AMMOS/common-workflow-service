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

import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class SchedulerQueueUtils {

	private static final Logger log = LoggerFactory.getLogger(SchedulerQueueUtils.class);
	
	// TODO: make these values come from configuration
	
	@Value("${cws.broker.obj.name}") private String BROKER_OBJ_NAME;
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
			for (ObjectName queueName : getBrokerViewMBean().getQueues()) {
				QueueViewMBean queueMbean = getQueueViewMBean(queueName);
				System.out.println("  "+queueMbean.getName() + 
						" : [enqueues: " + queueMbean.getEnqueueCount() +
						", dequeues: " + queueMbean.getDequeueCount() +
						", inFlights: " + queueMbean.getInFlightCount()+"]");
			}
			System.out.println("------------------------------------------------------------------------");
		} catch (Exception e) {
			log.error("failed to log scheduler queue", e);
		}
	}
	
	
	/**
	 * 
	 */
	public Set<org.apache.activemq.broker.Connection>  getAmqClients() throws Exception {
		Set<org.apache.activemq.broker.Connection> uniqueClients = new HashSet<org.apache.activemq.broker.Connection>();
		Map<String,BrokerService> map = BrokerRegistry.getInstance().getBrokers();
		BrokerService brokerService = map.get("cwsConsoleBroker");
		org.apache.activemq.broker.Connection[] clients = brokerService.getBroker().getClients();
		for (org.apache.activemq.broker.Connection client :  clients) {
			log.trace("CLIENT: "+client);
			log.trace("  stats: "+client.getStatistics());
			log.trace("  connId: "+client.getConnectionId());
			log.trace("  remoteAddr: "+client.getRemoteAddress());
			log.trace("  connector: "+client.getConnector());
			uniqueClients.add(client);
		}
		return uniqueClients;
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
		
		for (ObjectName existingQueueName : getBrokerViewMBean().getQueues()) {
			QueueViewMBean queueMbean = getQueueViewMBean(existingQueueName);
			if (queueMbean.getName().equals(queueName)) {
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
		String operationName="addQueue";
		Object[] params = {queueName};
		String[] sig = {"java.lang.String"};
		getConn().invoke(getActiveMQ(), operationName, params, sig);
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
	private BrokerViewMBean getBrokerViewMBean() throws Exception {
		return (BrokerViewMBean) MBeanServerInvocationHandler.newProxyInstance(
				getConn(), getActiveMQ(), BrokerViewMBean.class, true);
	}
	
	/**
	 * 
	 */
	private QueueViewMBean getQueueViewMBean(ObjectName queueName) throws Exception {
		return (QueueViewMBean) MBeanServerInvocationHandler.newProxyInstance(
				getConn(), queueName, QueueViewMBean.class, true);
	}
	
}
