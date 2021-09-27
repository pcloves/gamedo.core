package org.gamedo.logging;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;

public interface IEntityIdConverter {
    /**
     * 转换为唯一Id
     *
     * @param object 要转换的object
     * @return 返回唯一Id
     */
    default String convert(Object object) {

        final String id;

        if (object instanceof IEntity) {
            id = ((IEntity) object).getId();
        } else if (object instanceof IComponent) {
            id = ((IComponent<?>) object).getOwner().getId();
        } else {
            id = String.valueOf(object.hashCode());
        }

        return id;
    }

}
