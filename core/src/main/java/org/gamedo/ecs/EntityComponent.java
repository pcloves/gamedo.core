package org.gamedo.ecs;

import org.gamedo.ecs.interfaces.IEntity;

public class EntityComponent extends Component<IEntity> {

    public EntityComponent(IEntity owner) {
        super(owner);
    }

    @Override
    public IEntity getOwner() {
        return owner;
    }

}
