package org.gamedo.ecs;

import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;

import java.util.Optional;

@Slf4j
public abstract class Component<T extends IEntity> implements IComponent<T> {

    protected final T owner;

    protected Component(T owner) {
        this.owner = owner;
    }

    @Override
    public T getOwner() {
        return owner;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Optional<R> getInterface(Class<R> clazz) {
        return Optional.ofNullable(clazz.isInstance(this) ? (R) this : null);
    }
}
