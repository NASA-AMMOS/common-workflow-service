package jpl.cws.core.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import jakarta.jms.ConnectionFactory;
import jpl.cws.core.util.NettyShutdownUtil;

/**
 * Custom CachingConnectionFactory that provides better shutdown handling
 * for ActiveMQ Artemis to prevent NoClassDefFoundError during application shutdown.
 * 
 * This factory adds a delay during destruction to allow Netty threads
 * to terminate gracefully before the web application classloader is destroyed.
 */
public class CwsCachingConnectionFactory extends CachingConnectionFactory {
    
    private static final Logger log = LoggerFactory.getLogger(CwsCachingConnectionFactory.class);
    private static final long SHUTDOWN_DELAY_MS = 5000; // 5 seconds delay
    
    public CwsCachingConnectionFactory() {
        super();
    }
    
    public CwsCachingConnectionFactory(ConnectionFactory targetConnectionFactory) {
        super(targetConnectionFactory);
    }
    
    public CwsCachingConnectionFactory(ConnectionFactory targetConnectionFactory, int sessionCacheSize) {
        super(targetConnectionFactory);
        setSessionCacheSize(sessionCacheSize);
    }
    
    @Override
    public void destroy() {
        log.info("Starting destruction...");
        
        try {
            // First, call the parent destroy method
            super.destroy();
            log.info("Parent destroy completed");
            
            // Shutdown MySQL connection cleanup threads
            NettyShutdownUtil.shutdownMysqlConnectionCleanup();
            
            // Add a delay to allow Netty threads to terminate gracefully
            log.info("Waiting {}ms for Netty threads to terminate...", SHUTDOWN_DELAY_MS);
            Thread.sleep(SHUTDOWN_DELAY_MS);
            
            log.info("Destruction completed successfully");
            
        } catch (InterruptedException e) {
            log.warn("Destruction interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during destruction: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void resetConnection() {
        log.info("Resetting connection...");
        try {
            super.resetConnection();
            log.info("Connection reset completed");
        } catch (Exception e) {
            log.error("Error during connection reset: {}", e.getMessage(), e);
        }
    }
}
