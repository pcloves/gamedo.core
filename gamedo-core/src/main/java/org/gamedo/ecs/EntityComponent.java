package org.gamedo.ecs;

import org.gamedo.ecs.interfaces.IEntity;

@SuppressWarnings("unused")
public class EntityComponent extends Component<IEntity> {
    public EntityComponent(IEntity owner) {
        super(owner);
    }

    public EntityComponent() {
    }
}
