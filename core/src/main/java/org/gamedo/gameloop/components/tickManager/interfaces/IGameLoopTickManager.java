package org.gamedo.gameloop.components.tickManager.interfaces;

import org.gamedo.annotation.Tick;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * {@link IGameLoop}的心跳管理组件，可以为任意Object动态提供心跳逻辑
 */
public interface IGameLoopTickManager extends IComponent<IGameLoop> {

    /**
     * 将Object内所有拥有{@link Tick}注解的心跳函数进行注册，之后该Object内所有{@link Tick}方法都具备心跳逻辑
     *
     * @param object 要注册的类的实体
     * @return 返回成功注册的方法的数量
     * @see Tick
     */
    int register(Object object);

    /**
     * 将某个标注了{@link Tick}的心跳函数进行注册
     *
     * @param object 要注册的类的实体
     * @param method 要注册的方法
     * @return 注册成功返回true，如果method没有{@link Tick}注解者已经注册过，返回false
     */
    boolean register(Object object, Method method);

    /**
     * 将某个标注了{@link Tick}的心跳函数进行注册
     *
     * @param object   要注册的类的实体
     * @param method   要注册的方法
     * @param delay    心跳延迟开启时间
     * @param tick     心跳间隔
     * @param timeUnit 心跳时间单位
     * @param scheduleWithFixedDelay 是否以scheduleWithFixedDelay方式心跳
     * @return 注册成功返回true，如果该方法已经被{@link Tick}注解标准或者已经注册过，返回false
     */
    boolean register(Object object, Method method, long delay, long tick, TimeUnit timeUnit, boolean scheduleWithFixedDelay);

    /**
     * 取消某个类所有的心跳函数的注册
     *
     * @param object 要取消注册的类的实体
     * @return 返回取消注册成功的方法的数量
     */
    int unregister(Object object);

    /**
     * 取消某个类的某个心跳函数的注册
     *
     * @param object 要取消注册的类的实体
     * @param method 要取消的类型
     * @return 取消成功返回true，如果该方法之前没有被注册过，则返回false
     */
    boolean unregister(Object object, Method method);

    /**
     * 反注册所有的心跳函数
     *
     * @return 反注册成功的心跳函数的总数量
     */
    int unregisterAll();

}
