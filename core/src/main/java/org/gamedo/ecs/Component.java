package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;

import java.util.Optional;

@Log4j2
public abstract class Component<T extends IEntity> implements IComponent<T> {

    protected final T owner;

    protected Component(T owner) {
        this.owner = owner;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Optional<R> getInterface(Class<R> clazz) {
        return Optional.ofNullable(clazz.isInstance(this) ? (R) this : null);
    }
}
