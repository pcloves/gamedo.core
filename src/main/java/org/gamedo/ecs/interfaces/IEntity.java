package org.gamedo.ecs.interfaces;

import java.util.Optional;

public interface IEntity extends ITickable
{
    String getId();

    boolean hasComponent(Class<IComponent> clazz);

    <T extends IComponent> Optional<T> getComponent(Class<T> clazz);

    <T extends IComponent> Optional<T> addComponent(Class<T> clazz, T component);
}
