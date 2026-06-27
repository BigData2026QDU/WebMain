package org.bigdata.util;

import org.bigdata.tool.ServicePoolManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServicePoolManagerTest {

    @Test
    void getInstance_shouldReturnSameInstance() {
        ServicePoolManager instance1 = ServicePoolManager.getInstance();
        ServicePoolManager instance2 = ServicePoolManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void registerService_shouldRegisterSuccessfully() {
        ServicePoolManager manager = ServicePoolManager.getInstance();
        manager.registerService(String.class, () -> "test", 2);
        assertTrue(manager.isRegistered(String.class));
    }

    @Test
    void borrowService_shouldReturnRegisteredService() {
        ServicePoolManager manager = ServicePoolManager.getInstance();
        manager.registerService(Integer.class, () -> 42, 2);
        Integer service = manager.borrowService(Integer.class);
        assertNotNull(service);
        assertEquals(42, service);
        manager.returnService(Integer.class, service);
    }
}
