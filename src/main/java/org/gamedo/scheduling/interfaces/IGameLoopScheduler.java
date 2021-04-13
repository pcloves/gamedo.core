package org.gamedo.scheduling.interfaces;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.scheduling.GameLoopScheduled;

import java.lang.reflect.Method;

/**
 * {@link IGameLoop}的cron调度器组件，提供该{@link IGameLoop}线程内的cron调度的注册、反注册的管理功能
 *
 * @see GameLoopScheduled
 */
public interface IGameLoopScheduler extends IComponent {

    /**
     * 将Object内所有拥有{@link GameLoopScheduled}注解的方法（称之为cron方法）注册到调度注册器中，之后该Object就可以实现{@link IGameLoop}线程内的
     * cron调度，对于cron方法的定义要求，参考：{@link GameLoopScheduled}
     *
     * @param object 要注册的类的实体
     * @return 返回成功注册的方法的数量
     * @see GameLoopScheduled
     */
    int register(Object object);

    /**
     * 将某个标注了{@link GameLoopScheduled}的cron方法注册到调度注册器中
     *
     * @param object 要注册的类的实体
     * @param method 要注册的方法
     * @return 注册成功返回true，如果method没有要求的注解或者已经注册过，返回false
     */
    boolean register(Object object, Method method);

    /**
     * 将某个没有标注{@link GameLoopScheduled}的方法注册到调度注册器中
     *
     * @param object 要注册的类的实体
     * @param method 要注册的方法
     * @param cron   触发调度的cron表达式，配置方式可以参考：{@link GameLoopScheduled#value()}
     * @return 注册成功返回true，如果method已经被标注注解或者已经注册过，返回false
     */
    boolean register(Object object, Method method, String cron);

    /**
     * 取消某个类在调度注册器里的调度
     *
     * @param clazz 要取消注册的类型
     * @return 返回取消注册成功的方法的数量
     */
    int unregister(Class<?> clazz);

    /**
     * 取消某个类的某个方法在调度注册器中的调度
     *
     * @param clazz  要取消注册的类型
     * @param method 要取消的类型
     * @return 取消成功返回true，如果该方法之前没有被注册过，则返回false
     */
    boolean unregister(Class<?> clazz, Method method);

    /**
     * 反注册所有的调度
     *
     * @return 反注册成功的方法的总数量
     */
    int unregisterAll();
}
