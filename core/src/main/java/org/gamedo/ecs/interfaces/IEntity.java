package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Map;
import java.util.Optional;

public interface IEntity {
    /**
     * 本实体的唯一Id
     * @return 唯一Id
     */
    String getId();

    /**
     * 是否已经注册到某个{@link IGameLoop}线程上
     * @return true表示已经注册
     */
    boolean hasRegistered();

    /**
     * CAS方式更新注册状态，防止被注册到多个{@link IGameLoop}上
     * 话说增加这么一个接口实在是不雅观，还需要考虑考虑
     * @param expectedValue 期待值
     * @param newValue 更新值
     * @return true表示cas更新成功，false表示CAS失败
     */
    boolean casUpdateRegistered(boolean expectedValue, boolean newValue);
    /**
     * 是否拥有某种类型的组件
     *
     * @param clazz 要检测的组件
     * @param <T> 组件的类型
     * @return true表示拥有该组件
     * @see IEntity#addComponent(Class, Object)
     */
    <T> boolean hasComponent(Class<T> clazz);

    /**
     * 返回某种类型的组件
     *
     * @param <T>   组件的类型
     * @param clazz 要获取的类型
     * @return 如果没有指定类型的组件，返回{@link Optional#empty()}
     */
    <T> Optional<T> getComponent(Class<T> clazz);

    /**
     * 返回一个无法被修改的{@link java.util.Collections#unmodifiableMap(Map) Map}副本
     * @return 组件的类型 : value: 组件本身
     */
    Map<Class<?>, Object> getComponentMap();

    /**
     * 添加一个组件到该实体上，使之具备了该组件的功能，该方法只是将clazz作为key，将component作为value添加到一个Map中，并不会将clazz的父接口以
     * 及祖先接口添加到Map中，因此假如某组件的继承关系为：<br>
     * IComponent &lt;-- A &lt;-- B &lt;-- ComponentImpl<br>
     * 当调用{@link IEntity#addComponent}将B添加到实体中后，只能通过本方法获取到B，而无无法
     * 获取到A，除非也将A加入到实体中
     *
     * @param clazz     该组件的类型
     * @param component 要添加的组件
     * @param <T>       组件要暴露给外界的接口类型
     * @param <R>       组件的真正实现类型
     * @return 如果之前存在相同clazz的组件，则返回旧组件的{@link Optional}，否则返回{@link Optional#empty()}
     */
    <T, R extends T> Optional<T> addComponent(Class<T> clazz, R component);

}
