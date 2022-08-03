package org.gamedo.ecs.interfaces;

import org.gamedo.ecs.Entity;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Optional;

/**
 * 实体接口，代表了一个组合了多个组件（{@link IComponent}）的实体，在gamedo.core的ecs架构下，组件是逻辑功能的真正承载者，实体只是将
 * 形形色色具备各种功能的组件进行了组合，在默认实现{@link Entity}中，提供了一个构造函数：{@link Entity#Entity(String, Map)}，这意味
 * 着：在该构造函数的构造期间，实体就可以通过{@link IEntity#getComponent(Class)}获取到组件。<p>
 * 在某些情况下，当实体进行构造时，某些组件可能还未实例化，这就需要再实体实例化后，动态为其增加组件（{@link IEntity#addComponent(Class, Object)}），
 * 这带来的负面效果为：组件的归属周期（{@link IComponent#getOwner()}）和组件的生命周期并非完全契合：在组件实例化之后成为实体组件之前，
 * 调用{@link IComponent#getOwner()}时都会得到null。为了弱化这种不确定，gamedo.core建议动态增加{@link IComponent}组件时，按照如下
 * 流程进行：
 * <ul>
 * <li> 调用{@link IComponent#setOwner(IEntity)}设置实体归属，当且仅当返回false时执行下一步
 * <li> 调用{@link IEntity#addComponent(Class, Object)}，在默认实现类{@link Entity}中，当检测到要添加的组件为{@link IComponent}
 * 类型时，首先会检测{@link IComponent#getOwner()}是否就是当前实体，只有符合条件时，才执行下面的操作
 * </ul>
 * 动态删除{@link IComponent}组件时，建议执行如下流程：
 * <ul>
 * <li> 调用{@link IEntity#removeComponent(Class)}，将组件从实体中删除
 * <li> 调用{@link IComponent#setOwner(IEntity)}设置实体归属为null，且这两个步骤不能调换
 * </ul>
 */
@SuppressWarnings("unused")
public interface IEntity extends IInterfaceQueryable, IIdentity {

    default String getCategory() {
        return "Entity";
    }

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
     * @return 添加成功返回true；如果组件已经存在或者参数检测失败，则返回false；如果组件类型为{@link IComponent}，但是已经有归属且并非当前
     * 实体，返回false；如果其归属为空，并且如果能添加成功，则将其owner设为本实体
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
