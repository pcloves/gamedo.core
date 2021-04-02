package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Optional;

public interface IEntity extends ITickable {
    /**
     * 本实体的唯一Id
     */
    String getId();

    /**
     * 本实体当前归属的{@link IGameLoop}，如果在其他线程调用该函数，可能会返回{@link Optional#empty()}
     */
    default Optional<IGameLoop> gameLoop() {
        return IGameLoop.currentGameLoop();
    }

    /**
     * 是否拥有某种类型的组件
     *
     * @param clazz 要检测的组件
     * @see IEntity#addComponent(Class, IComponent)
     */
    boolean hasComponent(Class<IComponent> clazz);

    /**
     * 返回某种类型的组件
     *
     * @param clazz 要获取的类型
     * @param <T>   组件的类型
     * @return 如果没有指定类型的组件，返回{@link Optional#empty()}
     */
    <T extends IComponent> Optional<T> getComponent(Class<T> clazz);

    /**
     * 添加一个组件到该实体上，使之具备了该组件的功能，该方法只是将clazz作为key，将component作为value添加到一个Map中，并不会将clazz的父接口以
     * 及祖先接口添加到Map中，因此假如某组件的继承关系为：<br/>
     * IComponent <-- A <-- B <-- ComponentImpl<br/>
     * 当调用{@link IEntity#addComponent}将B添加到实体中后，只能通过本方法获取到B，而无无法
     * 获取到A，除非也将A加入到实体中
     *
     * @param clazz     该组件的类型
     * @param component 要添加的组件
     * @param <T>       组件的类型
     * @return 如果之前存在相同clazz的组件，则返回旧组件的{@link Optional}，否则返回{@link Optional#empty()}
     */
    <T extends IComponent> Optional<T> addComponent(Class<T> clazz, T component);
}
