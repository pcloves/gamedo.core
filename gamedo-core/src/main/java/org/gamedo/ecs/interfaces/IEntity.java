package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public interface IEntity extends IIdentity {
    /**
     * 是否拥有某种类型的组件
     *
     * @param interfaceClazz 要检测的组件
     * @param <T>            组件的类型
     * @return true表示拥有该组件
     * @see IEntity#addComponent(Class, Object)
     */
    <T> boolean hasComponent(Class<T> interfaceClazz);

    /**
     * 返回某种类型的组件
     *
     * @param <T>            组件的类型
     * @param interfaceClazz 要获取的类型
     * @return 如果没有指定类型的组件，返回{@link Optional#empty()}
     */
    <T> Optional<T> getComponent(Class<T> interfaceClazz);

    /**
     * 返回一个无法被修改的{@link Collections#unmodifiableMap(Map) Collections#unmodifiableMap}实时镜像<p>
     * 如果{@link IEntity}已经注册到某个{@link IGameLoop}线程，禁止在其他{@link IGameLoop}外部线程以任何形式的调用（包括安全发布），原
     * 因：
     * <ul>
     * <li> 外部线程虽然能获取到该{@link Map}，但是由于JMM的缘故，内部线程的修改不能保证对于外部线程的内存可见性
     * <li> 更严重的是：一旦外部线程获取该{@link Map}，就存在修改{@link Map#values()}组件数据的可能和风险，这在多线程并发场景下，是绝对禁
     * 止的
     * <li> 外部线程在遍历的同时，由于内部线程对{@link Map}进行了增加、删除，会导致外部线程抛出{@link ConcurrentModificationException}
     * 异常
     * </ul>
     *
     * @return 组件的类型 : value: 组件本身
     */
    Map<Class<?>, Object> getComponentMap();

    /**
     * 添加一个组件到该实体上，使之具备了该组件的功能，该方法只是将clazz作为key，将component作为value添加到一个Map中，并不会将clazz的父接口
     * 以及祖先接口添加到Map中，因此假如某组件的继承关系为：<br>
     * IComponent &lt;-- A &lt;-- B &lt;-- ComponentImpl<br>
     * 当调用本方法将B添加到实体中后，只能通过本方法获取到B，而无无法
     * 获取到A，除非也将A加入到实体中
     *
     * @param <T>            组件要暴露给外界的接口类型
     * @param <R>            组件的真正实现类型
     * @param interfaceClazz 该组件的类型
     * @param component      要添加的组件
     * @return 添加成功返回true， 如果组件已经存在或者参数检测失败，则返回false
     */
    <T, R extends T> boolean addComponent(Class<T> interfaceClazz, R component);

    /**
     * @param interfaceClazz 该组件的类型
     * @param <T>            组件要暴露给外界的接口类型
     * @param <R>            组件的真正实现类型
     * @return 返回被删除的组件，如果组件不存在，则返回{@link Optional#empty()}
     */
    <T, R extends T> Optional<R> removeComponent(Class<T> interfaceClazz);
}
