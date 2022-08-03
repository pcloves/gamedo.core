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
            id = ((IComponent<?>) object).getId();
        } else if (object instanceof String) {
            id = (String) object;
        } else if (object instanceof Number) {
            id = String.valueOf(((Number) object).doubleValue());
        } else if (object != null) {
            id = String.valueOf(object.hashCode());
        } else {
            id = "NullPointer";
        }

        return id;
    }

}
