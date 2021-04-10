package org.gamedo.scheduling.interfaces;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.scheduling.CronScheduled;

import java.lang.reflect.Method;

/**
 * cron调度器，提供在某个{@link IGameLoop}线程上，执行cron调度的功能
 * @see CronScheduled#value()
 */
public interface IScheduler extends IComponent {

    /**
     * 将某拥有标注{@link CronScheduled}的无参方法的类注册到调度注册器中，那么该组件就可以实现线程（也即{@link IGameLoop}）
     * 内的cron调度
     *
     * @param object 要注册的类的实体
     * @return 返回成功注册的方法的数量
     */
    int register(Object object);

    /**
     * 将某个标注了{@link CronScheduled}的无参方法注册到调度注册器中
     *
     * @param object 要注册的类的实体
     * @param method 要注册的方法
     * @return 注册成功返回true，如果method没有要求的注解或者已经注册过，返回false
     */
    boolean register(Object object, Method method);

    /**
     * 将某个没有标注{@link CronScheduled}的无参方法注册到调度注册器中
     *
     * @param object 要注册的类的实体
     * @param method 要注册的方法
     * @param cron   触发调度的cron表达式，配置方式可以参考：{@link CronScheduled#value()}
     * @return 注册成功返回true，如果method已经实现了注解或者已经注册过，返回false
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

    /**
     * 派发一次cron调度
     *
     * @param cron 要执行调度的cron语句
     * @return 被成功调度的方法的数量
     */
    int schedule(String cron);
}
