
package org.gamedo.ecs;

import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.exception.GamedoException;
import org.gamedo.logging.Markers;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@EqualsAndHashCode(of = "id")
@Log4j2
public class Entity implements IEntity {
    protected final String id;
    protected final Map<Class<?>, Object> componentMap;
    private String toString = "invalid";

    @SuppressWarnings("unchecked")
    public Entity(final String id, Map<Class<?>, Object> componentMap) {
        this.id = id;
        this.componentMap = new HashMap<>(componentMap == null ? new HashMap<>() : componentMap);

        @SuppressWarnings("rawtypes") final List<Object> failedList = this.componentMap.values().stream()
                .filter(value -> value instanceof IComponent)
                .map(value -> (IComponent)value)
                .peek(iComponent -> iComponent.setOwner(this))
                .filter(iComponent -> iComponent.getOwner() != this)
                .collect(Collectors.toList());

        if (!failedList.isEmpty()) {
            log.error(Markers.GameLoopEntityManager, "setOwner failed when owner Entity initiated, list:{}", failedList);
        }

        updateToString();
    }

    public Entity(String id) {
        this(id, null);
    }

    public Entity() {
        this(UUID.randomUUID().toString(), null);
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

    @SuppressWarnings("unchecked")
    @Override
    public <T, R extends T> boolean addComponent(Class<T> interfaceClazz, R component) {

        if (!interfaceClazz.isInstance(component)) {
            throw new GamedoException("illegal interface clazz:" + interfaceClazz.getName() + ", instance clazz:" +
                    component.getClass().getName());
        }

        if (componentMap.containsKey(interfaceClazz)) {
            return false;
        }

        if (component instanceof IComponent) {
            final IComponent<IEntity> com = (IComponent<IEntity>) component;
            if (com.getOwner() != null) {
                if (com.getOwner() != this) {
                    log.error(Markers.GameLoopEntityManager, "IComponent.setOwner should called before addComponent, component clazz:{}",
                            component.getClass().getName());
                    return false;
                }
            }
            else {
                com.setOwner(this);
            }
        }

        componentMap.put(interfaceClazz, component);
        updateToString();

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R extends T> Optional<R> removeComponent(Class<T> interfaceClazz) {

        final R component = (R) componentMap.remove(interfaceClazz);
        final Optional<R> optional = Optional.ofNullable(component);
        updateToString();

        return optional;
    }

    @Override
    public String toString() {
        return toString;
    }

    private void updateToString() {
        toString = "Entity{" +
                "id=" + id +
                ", category=" + getCategory() +
                ", componentCount=" + this.componentMap.size() +
                '}';
    }
}
