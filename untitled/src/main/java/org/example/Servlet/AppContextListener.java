package org.example.Servlet;

import org.example.pool.ServicePoolManager;
import org.example.service.UserService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Application context listener for initializing and cleaning up service pools.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final int USER_SERVICE_POOL_SIZE = 16;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Initializing service pools...");

        ServicePoolManager poolManager = ServicePoolManager.getInstance();

        // Register UserService to the pool
        poolManager.registerService(
            UserService.class,
            UserService::new,
            USER_SERVICE_POOL_SIZE
        );

        System.out.println("Service pools initialized. UserService pool size: " + USER_SERVICE_POOL_SIZE);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Shutting down service pools...");
        ServicePoolManager.getInstance().shutdown();
        System.out.println("Service pools shut down.");
    }
}
