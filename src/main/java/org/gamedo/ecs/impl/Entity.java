package org.gamedo.ecs.impl;

import lombok.EqualsAndHashCode;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EqualsAndHashCode(of = "id")
public class Entity implements IEntity
{
    private final String id;
    private final Map<Class<? extends IComponent>, IComponent> componentMap;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Entity(final String id, Optional<Map<Class<? extends IComponent>, IComponent>> optionalMap) {
        this.id = id;
        componentMap = new HashMap<>(optionalMap.orElse(Collections.emptyMap()));
    }

    public Entity(String id) {
        this(id, Optional.empty());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean hasComponent(Class<IComponent> clazz) {
        return componentMap.containsKey(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IComponent> Optional<T> getComponent(Class<T> clazz) {
        return Optional.ofNullable((T) componentMap.get(clazz));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IComponent> Optional<T> addComponent(Class<T> clazz, T component) {
        return (Optional<T>) Optional.ofNullable(componentMap.put(clazz, component));
    }

    @Override
    public void tick(long elapse) {

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
