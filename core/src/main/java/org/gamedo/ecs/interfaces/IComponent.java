package org.gamedo.ecs.interfaces;

public interface IComponent<T extends IEntity> extends IInterfaceQueryable {

    T getOwner();
}
