package org.gamedo.util.function;

import org.gamedo.ecs.interfaces.IEntity;

public interface IEntityFunction {

    static <T extends IEntity> EntityPredicate<T> hasComponent(Class<?> clazz) {
        return entity -> entity.hasComponent(clazz);
    }
}
