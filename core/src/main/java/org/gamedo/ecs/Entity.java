package org.gamedo.ecs;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.interfaces.IEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@EqualsAndHashCode(of = "id")
@Slf4j
public class Entity implements IEntity {
    protected final String id;
    protected final Map<Class<?>, Object> componentMap;

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
