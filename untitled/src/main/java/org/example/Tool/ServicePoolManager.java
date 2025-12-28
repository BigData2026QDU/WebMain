package org.example.Tool;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ServicePoolManager {
    private static final int DEFAULT_POOL_SIZE = 32;
    private static final ServicePoolManager INSTANCE = new ServicePoolManager();

    private final ConcurrentMap<Class<?>, GenericObjectPool<?>> pools = new ConcurrentHashMap<>();

    private ServicePoolManager() {
    }

    public static ServicePoolManager getInstance() {
        return INSTANCE;
    }

    public <T> void registerService(Class<T> serviceClass, Supplier<T> creator) {
        registerService(serviceClass, creator, DEFAULT_POOL_SIZE, null);
    }

    public <T> void registerService(Class<T> serviceClass, Supplier<T> creator, int poolSize) {
        registerService(serviceClass, creator, poolSize, null);
    }

    public <T> void registerService(Class<T> serviceClass,
                                    Supplier<T> creator,
                                    int poolSize,
                                    Consumer<T> destroyer) {
        Objects.requireNonNull(serviceClass, "serviceClass");
        Objects.requireNonNull(creator, "creator");
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be positive");
        }
        GenericObjectPoolConfig<T> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(poolSize);
        config.setMaxIdle(poolSize);
        config.setMinIdle(poolSize);

        GenericObjectPool<T> pool = new GenericObjectPool<>(
            new ServicePooledObjectFactory<>(creator, destroyer),
            config
        );
        try {
            pool.preparePool();
        } catch (Exception e) {
            pool.close();
            throw new RuntimeException("Preload pool failed: " + serviceClass.getName(), e);
        }

        GenericObjectPool<?> existing = pools.putIfAbsent(serviceClass, pool);
        if (existing != null) {
            pool.close();
            throw new IllegalStateException("Service already registered: " + serviceClass.getName());
        }
    }

    public <T> T borrowService(Class<T> serviceClass) {
        GenericObjectPool<T> pool = getPool(serviceClass);
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException("Borrow Service failed: " + serviceClass.getName(), e);
        }
    }

    public <T> void returnService(Class<T> serviceClass, T service) {
        Objects.requireNonNull(service, "Service");
        GenericObjectPool<T> pool = getPool(serviceClass);
        pool.returnObject(service);
    }

    public <T> void invalidateService(Class<T> serviceClass, T service) {
        Objects.requireNonNull(service, "Service");
        GenericObjectPool<T> pool = getPool(serviceClass);
        try {
            pool.invalidateObject(service);
        } catch (Exception e) {
            throw new RuntimeException("Invalidate Service failed: " + serviceClass.getName(), e);
        }
    }

    public boolean isRegistered(Class<?> serviceClass) {
        return pools.containsKey(serviceClass);
    }

    public void shutdown() {
        for (GenericObjectPool<?> pool : pools.values()) {
            pool.close();
        }
        pools.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> GenericObjectPool<T> getPool(Class<T> serviceClass) {
        Objects.requireNonNull(serviceClass, "serviceClass");
        GenericObjectPool<?> pool = pools.get(serviceClass);
        if (pool == null) {
            throw new IllegalStateException("Service not registered: " + serviceClass.getName());
        }
        return (GenericObjectPool<T>) pool;
    }
}
