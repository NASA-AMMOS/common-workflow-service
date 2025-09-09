package jpl.cws.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for properly shutting down Netty components to prevent
 * NoClassDefFoundError during application shutdown.
 * 
 * This is particularly important for ActiveMQ Artemis which uses Netty
 * for network communication.
 */
public class NettyShutdownUtil {
    
    private static final Logger log = LoggerFactory.getLogger(NettyShutdownUtil.class);
    private static final long SHUTDOWN_TIMEOUT_MS = 5000;
    private static volatile boolean shutdownHookRegistered = false;
    
    /**
     * Registers a JVM shutdown hook to ensure Netty threads are properly cleaned up
     * even if the servlet context listener doesn't get called.
     */
    public static synchronized void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("JVM shutdown hook triggered");
                shutdownMysqlConnectionCleanup();
                forceShutdownNettyThreads();
            }));
            shutdownHookRegistered = true;
            log.info("Shutdown hook registered");
        }
    }
    
    /**
     * Forces shutdown of all Netty-related threads and cleans up ThreadLocal resources.
     * This method is safe to call multiple times.
     */
    public static void forceShutdownNettyThreads() {
        log.info("Starting forced Netty thread shutdown...");
        
        try {
            // First, try to clean up Netty ThreadLocal resources
            cleanupNettyThreadLocals();
            
            // Then try to interrupt all Netty threads
            Thread.getAllStackTraces().keySet().stream()
                .filter(t -> isNettyThread(t))
                .forEach(thread -> {
                    log.debug("Interrupting thread: {}", thread.getName());
                    thread.interrupt();
                });
            
            // Give threads time to terminate gracefully
            Thread.sleep(1000);
            
            // Check if any Netty threads are still alive
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < SHUTDOWN_TIMEOUT_MS) {
                boolean hasNettyThreads = Thread.getAllStackTraces().keySet().stream()
                    .anyMatch(t -> isNettyThread(t) && t.isAlive());
                
                if (!hasNettyThreads) {
                    log.info("All Netty threads terminated gracefully");
                    return;
                }
                
                Thread.sleep(100);
            }
            
            // Force stop any remaining Netty threads using safer method
            log.warn("Force stopping remaining Netty threads...");
            Thread.getAllStackTraces().keySet().stream()
                .filter(t -> isNettyThread(t) && t.isAlive())
                .forEach(thread -> {
                    log.warn("Force stopping thread: {}", thread.getName());
                    try {
                        // Use interrupt instead of deprecated stop() method
                        thread.interrupt();
                        // Give thread a moment to respond to interrupt
                        Thread.sleep(100);
                        if (thread.isAlive()) {
                            log.warn("Thread {} did not respond to interrupt, marking as daemon", thread.getName());
                            // Mark as daemon so it doesn't prevent JVM shutdown
                            thread.setDaemon(true);
                        }
                    } catch (Exception e) {
                        log.error("Error force stopping thread {}: {}", thread.getName(), e.getMessage());
                    }
                });
                
        } catch (Exception e) {
            log.error("Error during Netty thread shutdown: {}", e.getMessage(), e);
        }
        
        log.info("Netty thread shutdown completed");
    }
    
    /**
     * Determines if a thread is a Netty-related thread.
     */
    private static boolean isNettyThread(Thread thread) {
        String name = thread.getName().toLowerCase();
        return name.contains("netty") || 
               name.contains("activemq-client-netty-threads") ||
               name.contains("artemis") ||
               name.contains("nioeventloop") ||
               name.contains("eventloop") ||
               name.contains("activemq-pageexecutor");
    }
    
    /**
     * Attempts to clean up Netty ThreadLocal resources to prevent memory leaks.
     */
    private static void cleanupNettyThreadLocals() {
        try {
            log.debug("Attempting to cleanup Netty ThreadLocal resources...");
            
            // Try to access Netty's InternalThreadLocalMap and clean it up
            try {
                Class<?> internalThreadLocalMapClass = Class.forName("io.netty.util.internal.InternalThreadLocalMap");
                Method getMethod = internalThreadLocalMapClass.getMethod("get");
                Method removeMethod = internalThreadLocalMapClass.getMethod("remove");
                
                // Get the current thread's InternalThreadLocalMap
                Object threadLocalMap = getMethod.invoke(null);
                if (threadLocalMap != null) {
                    log.debug("Found InternalThreadLocalMap, attempting cleanup...");
                    removeMethod.invoke(null);
                    log.debug("InternalThreadLocalMap cleanup completed");
                }
            } catch (Exception e) {
                log.debug("Could not access InternalThreadLocalMap: {}", e.getMessage());
            }
            
            // Try to clean up FastThreadLocal resources
            try {
                Class<?> fastThreadLocalClass = Class.forName("io.netty.util.concurrent.FastThreadLocal");
                Method removeAllMethod = fastThreadLocalClass.getMethod("removeAll");
                removeAllMethod.invoke(null);
                log.debug("FastThreadLocal cleanup completed");
            } catch (Exception e) {
                log.debug("Could not access FastThreadLocal: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error during ThreadLocal cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Shuts down MySQL connection cleanup threads to prevent memory leaks.
     */
    public static void shutdownMysqlConnectionCleanup() {
        try {
            log.info("Attempting to shutdown MySQL connection cleanup threads...");
            
            // Try to access MySQL's AbandonedConnectionCleanupThread
            try {
                Class<?> cleanupThreadClass = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
                Method checkedShutdownMethod = cleanupThreadClass.getMethod("checkedShutdown");
                checkedShutdownMethod.invoke(null);
                log.info("MySQL connection cleanup thread shutdown completed");
            } catch (Exception e) {
                log.debug("Could not shutdown MySQL cleanup thread: {}", e.getMessage());
            }
            
            // Also try to interrupt any MySQL-related threads
            Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().toLowerCase().contains("mysql") || 
                           t.getName().toLowerCase().contains("connection-cleanup"))
                .forEach(thread -> {
                    log.debug("Interrupting MySQL thread: {}", thread.getName());
                    thread.interrupt();
                });
                
        } catch (Exception e) {
            log.error("Error during MySQL cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Attempts to gracefully shutdown Netty event loop groups using reflection.
     * This method tries to access Netty components through various objects.
     */
    public static void shutdownNettyEventLoopGroups(Object target) {
        if (target == null) {
            return;
        }
        
        try {
            log.info("Attempting to shutdown Netty event loop groups...");
            
            // Look for common Netty event loop group field names
            String[] fieldNames = {
                "bossGroup", "workerGroup", "eventLoopGroup", "nioEventLoopGroup",
                "clientEventLoopGroup", "serverEventLoopGroup", "acceptorEventLoopGroup"
            };
            
            for (String fieldName : fieldNames) {
                try {
                    Field field = findField(target.getClass(), fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        Object eventLoopGroup = field.get(target);
                        
                        if (eventLoopGroup != null) {
                            log.debug("Found Netty event loop group: {}", fieldName);
                            shutdownEventLoopGroup(eventLoopGroup, fieldName);
                        }
                    }
                } catch (Exception e) {
                    // Field doesn't exist or can't be accessed, continue to next one
                }
            }
            
            // Also try to find nested Netty components
            findAndShutdownNestedNettyComponents(target);
            
        } catch (Exception e) {
            log.error("Error during event loop group shutdown: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Recursively finds and shuts down nested Netty components.
     */
    private static void findAndShutdownNestedNettyComponents(Object target) {
        try {
            Field[] fields = target.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().toLowerCase().contains("netty") || 
                    field.getName().toLowerCase().contains("eventloop") ||
                    field.getName().toLowerCase().contains("channel")) {
                    
                    field.setAccessible(true);
                    Object component = field.get(target);
                    
                    if (component != null) {
                        log.debug("Found potential Netty component: {}", field.getName());
                        shutdownEventLoopGroup(component, field.getName());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors during recursive search
        }
    }
    
    /**
     * Attempts to shutdown a single event loop group.
     */
    private static void shutdownEventLoopGroup(Object eventLoopGroup, String name) {
        try {
            // Try to call shutdownGracefully method
            try {
                eventLoopGroup.getClass().getMethod("shutdownGracefully").invoke(eventLoopGroup);
                log.debug("Called shutdownGracefully on {}", name);
            } catch (Exception e) {
                // Try alternative shutdown method
                try {
                    eventLoopGroup.getClass().getMethod("shutdown").invoke(eventLoopGroup);
                    log.debug("Called shutdown on {}", name);
                } catch (Exception e2) {
                    // Try with timeout parameter
                    try {
                        eventLoopGroup.getClass().getMethod("shutdownGracefully", long.class, long.class, TimeUnit.class)
                            .invoke(eventLoopGroup, 100L, 1000L, TimeUnit.MILLISECONDS);
                        log.debug("Called shutdownGracefully with timeout on {}", name);
                    } catch (Exception e3) {
                        log.debug("Could not shutdown {}: {}", name, e3.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error shutting down {}: {}", name, e.getMessage(), e);
        }
    }
    
    /**
     * Finds a field in the class hierarchy.
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    
    /**
     * Attempts to shutdown client-side Netty components using reflection.
     * 
     * @param connectionFactory The ActiveMQ connection factory to shutdown Netty components from
     */
    public static void shutdownClientNettyComponents(Object connectionFactory) {
        try {
            log.info("Attempting to shutdown client Netty components...");
            
            if (connectionFactory != null) {
                // Look for Netty components in the connection factory
                Field[] fields = connectionFactory.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.getName().toLowerCase().contains("netty") || 
                        field.getName().toLowerCase().contains("eventloop") ||
                        field.getName().toLowerCase().contains("channel")) {
                        
                        field.setAccessible(true);
                        Object component = field.get(connectionFactory);
                        
                        if (component != null) {
                            log.debug("Found potential Netty component: {}", field.getName());
                            
                            // Try to call shutdown methods
                            try {
                                component.getClass().getMethod("shutdownGracefully").invoke(component);
                                log.debug("Called shutdownGracefully on {}", field.getName());
                            } catch (Exception e) {
                                try {
                                    component.getClass().getMethod("shutdown").invoke(component);
                                    log.debug("Called shutdown on {}", field.getName());
                                } catch (Exception e2) {
                                    // Ignore if method doesn't exist
                                }
                            }
                        }
                    }
                }
                
                // Give components time to shutdown
                Thread.sleep(500);
            }
        } catch (Exception e) {
            log.error("Error during client Netty component shutdown: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Attempts to gracefully shutdown ActiveMQ Artemis thread pools to prevent IllegalMonitorStateException.
     * This method tries to shutdown thread pool executors before they are forcefully terminated.
     * 
     * @param activeMQServer The ActiveMQ Artemis server instance
     */
    public static void shutdownActiveMQThreadPools(Object activeMQServer) {
        if (activeMQServer == null) {
            return;
        }
        
        try {
            log.info("Attempting to shutdown ActiveMQ Artemis thread pools...");
            
            // Look for thread pool executors in the server
            Field[] fields = activeMQServer.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().toLowerCase().contains("executor") || 
                    field.getName().toLowerCase().contains("threadpool") ||
                    field.getName().toLowerCase().contains("pageexecutor")) {
                    
                    field.setAccessible(true);
                    Object executor = field.get(activeMQServer);
                    
                    if (executor != null) {
                        log.debug("Found potential thread pool executor: {}", field.getName());
                        shutdownThreadPoolExecutor(executor, field.getName());
                    }
                }
            }
            
            // Also look in nested objects
            findAndShutdownThreadPools(activeMQServer);
            
            // Give thread pools time to shutdown gracefully
            Thread.sleep(1000);
            
        } catch (Exception e) {
            log.error("Error during ActiveMQ thread pool shutdown: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Recursively finds and shuts down thread pool executors.
     */
    private static void findAndShutdownThreadPools(Object target) {
        try {
            Field[] fields = target.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().toLowerCase().contains("executor") || 
                    field.getName().toLowerCase().contains("threadpool") ||
                    field.getName().toLowerCase().contains("pageexecutor")) {
                    
                    field.setAccessible(true);
                    Object executor = field.get(target);
                    
                    if (executor != null) {
                        log.debug("Found nested thread pool executor: {}", field.getName());
                        shutdownThreadPoolExecutor(executor, field.getName());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors during recursive search
        }
    }
    
    /**
     * Attempts to shutdown a single thread pool executor.
     */
    private static void shutdownThreadPoolExecutor(Object executor, String name) {
        try {
            // Try to call shutdown method
            try {
                executor.getClass().getMethod("shutdown").invoke(executor);
                log.debug("Called shutdown on {}", name);
                
                // Wait for termination with timeout
                try {
                    Method awaitTerminationMethod = executor.getClass().getMethod("awaitTermination", long.class, TimeUnit.class);
                    boolean terminated = (Boolean) awaitTerminationMethod.invoke(executor, 2000L, TimeUnit.MILLISECONDS);
                    if (terminated) {
                        log.debug("Thread pool {} terminated gracefully", name);
                    } else {
                        log.warn("Thread pool {} did not terminate within timeout", name);
                        // Try shutdownNow as last resort
                        try {
                            executor.getClass().getMethod("shutdownNow").invoke(executor);
                            log.debug("Called shutdownNow on {}", name);
                        } catch (Exception e) {
                            log.debug("Could not call shutdownNow on {}: {}", name, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not await termination on {}: {}", name, e.getMessage());
                }
                
            } catch (Exception e) {
                log.debug("Could not shutdown {}: {}", name, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error shutting down thread pool {}: {}", name, e.getMessage(), e);
        }
    }
}
