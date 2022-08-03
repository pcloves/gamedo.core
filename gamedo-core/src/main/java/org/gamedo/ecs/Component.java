package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@Log4j2
public abstract class Component<T extends IEntity> implements IComponent<T> {

    protected final AtomicReference<T> ownerRef = new AtomicReference<>(null);

    protected Component(T owner) {
        this.ownerRef.set(owner);
    }

    protected Component() {
    }

    @Override
    public T getOwner() {
        return ownerRef.get();
    }

    @Override
    public boolean setOwner(T owner) {
        final T ownerOld = owner == null ?  getOwner() : null;
        return this.ownerRef.compareAndSet(ownerOld, owner);
    }
}
