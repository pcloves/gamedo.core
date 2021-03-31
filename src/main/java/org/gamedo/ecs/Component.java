package org.gamedo.ecs;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;

public class Component implements IComponent {

    private final IEntity owner;

    public Component(IEntity owner) {
        this.owner = owner;
    }

    @Override
    public String getOwnerId() {
        return owner.getId();
    }

    @Override
    public void tick(long elapse) {

    }
}
