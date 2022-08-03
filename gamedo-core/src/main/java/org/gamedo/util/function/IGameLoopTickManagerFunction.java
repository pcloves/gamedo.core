package org.gamedo.util.function;

import org.gamedo.annotation.Tick;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public interface IGameLoopTickManagerFunction {

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IGameLoopTickManager}组件注册心跳逻辑，object
     * 的所有包含{@link Tick}注解的无参函数都会被注册
     *
     * @param object 要注册调度的实例
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功注册的方法的数量
     */
    static GameLoopFunction<Integer> register(Object object) {
        return gameLoop -> gameLoop.getComponent(IGameLoopTickManager.class)
                .map(iGameLoopTickManager -> iGameLoopTickManager.register(object))
                .orElse(0);
    }

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IGameLoopTickManager}组件注册心跳函数
     *
     * @param object                 要注册的类的实体
     * @param method                 要注册的方法
     * @param delay                  心跳延迟开启时间
     * @param tick                   心跳间隔
     * @param timeUnit               心跳时间单位
     * @param scheduleWithFixedDelay 是否以scheduleWithFixedDelay方式心跳
     * @return 注册成功返回true，如果该方法已经被注册过或者所属线程已经shutdown，返回false
     */
    static GameLoopFunction<Boolean> register(Object object, Method method, long delay, long tick, TimeUnit timeUnit, boolean scheduleWithFixedDelay) {
        return gameLoop -> gameLoop.getComponent(IGameLoopTickManager.class)
                .map(iGameLoopTickManager -> iGameLoopTickManager.register(object, method, delay, tick, timeUnit, scheduleWithFixedDelay))
                .orElse(false);
    }

    /**
     * 定义一个行为：取消某个类的某个心跳函数的注册
     *
     * @param object 要取消注册的类的实体
     * @param method 要取消的类型
     * @return 取消成功返回true，如果该方法之前没有被注册过，则返回false
     */
    static GameLoopFunction<Boolean> unregister(Object object, Method method) {
        return gameLoop -> gameLoop.getComponent(IGameLoopTickManager.class)
                .map(iGameLoopTickManager -> iGameLoopTickManager.unregister(object, method))
                .orElse(false);
    }


    /**
     * 定义一个行为：将object内的所有{@link Tick}方法从{@link IGameLoopEventBus}取消注册
     *
     * @param object 要执行的object
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IGameLoopTickManager#unregister(Object)} 的返回值
     */
    static GameLoopFunction<Integer> unregister(Object object) {
        return gameLoop -> gameLoop.getComponent(IGameLoopTickManager.class)
                .map(iGameLoopTickManager -> iGameLoopTickManager.unregister(object))
                .orElse(0);
    }
}
