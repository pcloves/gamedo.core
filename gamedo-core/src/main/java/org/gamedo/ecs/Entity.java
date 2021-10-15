package org.gamedo.ecs;

import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.exception.GamedoException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@EqualsAndHashCode(of = "id")
@Log4j2
public class Entity implements IEntity {
    protected final String id;
    protected final Map<Class<?>, Object> componentMap;

    public Entity(final String id, Map<Class<?>, Object> componentMap) {
        this.id = id;
        this.componentMap = new HashMap<>(componentMap == null ? Collections.emptyMap() : componentMap);

        this.componentMap.forEach((key, value) -> {
            if (!key.isInstance(value)) {
                throw new GamedoException("illegal interface clazz:" + key.getName() + ", instance clazz:" +
                        value.getClass().getName());
            }
        });
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
    public <T> boolean hasComponent(Class<T> interfaceClazz) {
        return componentMap.containsKey(interfaceClazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getComponent(Class<T> interfaceClazz) {
        return Optional.ofNullable((T) componentMap.get(interfaceClazz));
    }

    @Override
    public Map<Class<?>, Object> getComponentMap() {
        return Collections.unmodifiableMap(componentMap);
    }

    @Override
    public <T, R extends T> boolean addComponent(Class<T> interfaceClazz, R component) {

        if (!interfaceClazz.isInstance(component)) {
            throw new GamedoException("illegal interface clazz:" + interfaceClazz.getName() + ", instance clazz:" +
                    component.getClass().getName());
        }

        if (componentMap.containsKey(interfaceClazz)) {
            return false;
        }

        return componentMap.put(interfaceClazz, component) == null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R extends T> Optional<R> removeComponent(Class<T> interfaceClazz) {

        final R component = (R) componentMap.remove(interfaceClazz);
        return Optional.ofNullable(component);
    }

    @Override
    public String toString() {
        //noinspection StringBufferReplaceableByString
        final StringBuilder sb = new StringBuilder("Entity{");
        sb.append("hashCode=").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", componentMap=").append(componentMap.keySet().stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toList()));
        sb.append('}');
        return sb.toString();
    }
}
