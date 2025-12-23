package org.example.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ServicePooledObjectFactory<T> extends BasePooledObjectFactory<T> {
    private final Supplier<T> creator;
    private final Consumer<T> destroyer;

    ServicePooledObjectFactory(Supplier<T> creator, Consumer<T> destroyer) {
        this.creator = Objects.requireNonNull(creator, "creator");
        this.destroyer = destroyer;
    }

    @Override
    public T create() {
        return creator.get();
    }

    @Override
    public PooledObject<T> wrap(T obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<T> p) throws Exception {
        T obj = p.getObject();
        if (destroyer != null) {
            destroyer.accept(obj);
            return;
        }
        if (obj instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
