package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;

import java.util.Optional;

@Log4j2
public class Component implements IComponent {

    protected final IEntity owner;

    public Component(IEntity owner) {
        this.owner = owner;
    }

    @Override
    public IEntity getOwner() {
        return owner;
    }

    @Override
    public void tick(long elapse) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getInterface(Class<T> clazz) {
        return Optional.ofNullable(clazz.isInstance(this) ? (T) this : null);
    }
}
