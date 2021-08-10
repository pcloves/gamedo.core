package org.gamedo.util.function;

import org.gamedo.annotation.Cron;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.reflect.Method;

public interface IGameLoopSchedulerFunction {

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IGameLoopScheduler}组件注册cron调度，object
     * 的所有包含{@link Cron}注解的无参函数都会被注册
     *
     * @param object 要注册调度的实例
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功注册的方法的数量
     */
    static GameLoopFunction<Integer> register(Object object) {
        return gameLoop -> gameLoop.getComponent(IGameLoopScheduler.class)
                .map(iScheduleRegister -> iScheduleRegister.register(object))
                .orElse(0);
    }

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IGameLoopScheduler}组件注册cron调度，并指定object
     * 的method方法以自定义的cron表达式进行注册
     *
     * @param object 要注册的实体
     * @param method 该实体的方法
     * @param cron   要注册的cron表达式
     * @return 返回该行为的定义，其中GameLoopFunction中的Boolean代表是否注册成功
     */
    static GameLoopFunction<Boolean> register(Object object, Method method, String cron) {
        return gameLoop -> gameLoop.getComponent(IGameLoopScheduler.class)
                .map(iScheduleRegister -> iScheduleRegister.register(object, method, cron))
                .orElse(false);
    }

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IGameLoopScheduler}取消注册cron调度
     *
     * @param clazz 要取消注册的类
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功取消注册的方法的数量
     */
    static GameLoopFunction<Integer> unregister(Class<?> clazz) {
        return gameLoop -> gameLoop.getComponent(IGameLoopScheduler.class)
                .map(iScheduleRegister -> iScheduleRegister.unregister(clazz))
                .orElse(0);
    }

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IGameLoopScheduler}取消注册cron调度
     *
     * @param clazz  要取消注册的类
     * @param method 要取消注册的方法
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功取消注册的方法的数量
     */
    static GameLoopFunction<Boolean> unregister(Class<?> clazz, Method method) {
        return gameLoop -> gameLoop.getComponent(IGameLoopScheduler.class)
                .map(iScheduleRegister -> iScheduleRegister.unregister(clazz, method))
                .orElse(false);
    }
}
