package org.bigdata.servlet;

import org.bigdata.tool.ServicePoolManager;
import org.bigdata.service.UserService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final int USER_SERVICE_POOL_SIZE = 16;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Initializing Service pools...");

        ServicePoolManager poolManager = ServicePoolManager.getInstance();

        poolManager.registerService(
            UserService.class,
            UserService::new,
            USER_SERVICE_POOL_SIZE
        );

        System.out.println("Service pools initialized. UserService pool size: " + USER_SERVICE_POOL_SIZE);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Shutting down Service pools...");
        ServicePoolManager.getInstance().shutdown();
        System.out.println("Service pools shut down.");
    }
}
