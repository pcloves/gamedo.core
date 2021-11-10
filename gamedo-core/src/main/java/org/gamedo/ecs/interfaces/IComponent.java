package org.gamedo.ecs.interfaces;

public interface IComponent<T extends IEntity> extends IInterfaceQueryable, IIdentity {

    /**
     * 获取该组件所归属的实体
     * @return 返回所属实体
     */
    T getOwner();

    /**
     * 默认返回所属实体的唯一id
     * @return 返回所属实体的身份id
     */
    @Override
    default String getId() {
        return getOwner() != null ? getOwner().getId() : "";
    }
}
