package org.gamedo.ecs;

import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.interfaces.IEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@EqualsAndHashCode(of = "id")
@Log4j2
public class Entity implements IEntity {
    private final String id;
    protected final Map<Class<?>, Object> componentMap;
    /**
     * 防止同一个Entity被注册到多个GameLoop上
     */
    private final AtomicBoolean hasRegistered = new AtomicBoolean(false);

    public Entity(final String id, Map<Class<?>, Object> componentMap) {
        this.id = id;
        this.componentMap = new HashMap<>(componentMap == null ? Collections.emptyMap() : componentMap);
    }

    public Entity(String id) {
        this(id, null);
    }

    @SuppressWarnings("unused")
    public Entity(Supplier<String> idSupplier) {
        this(idSupplier.get());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean hasRegistered() {
        return hasRegistered.get();
    }

    @Override
    public boolean casUpdateRegistered(boolean expectedValue, boolean newValue) {
        return hasRegistered.compareAndSet(expectedValue, newValue);
    }

    @Override
    public <T> boolean hasComponent(Class<T> clazz) {
        return componentMap.containsKey(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getComponent(Class<T> clazz) {
        return Optional.ofNullable((T) componentMap.get(clazz));
    }

    @Override
    public Map<Class<?>, Object> getComponentMap() {
        return Collections.unmodifiableMap(componentMap);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R extends T> Optional<T> addComponent(Class<T> clazz, R component) {

        return (Optional<T>) Optional.ofNullable(componentMap.put(clazz, component));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Entity{");
        sb.append("id='").append(id).append('\'');
        sb.append(", componentMap=").append(componentMap.keySet());
        sb.append('}');
        return sb.toString();
    }
}
