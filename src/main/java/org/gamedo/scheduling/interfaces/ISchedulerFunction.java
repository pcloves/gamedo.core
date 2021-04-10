package org.gamedo.scheduling.interfaces;

import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

public interface ISchedulerFunction {

    /**
     * 定义一个行为：指定{@link IScheduler}强行执行一次调度，所有注册到本{@link IScheduler}中且cron表达式一致的函数都会被触发
     *
     * @param cron 执行的cron表达式
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功调度的方法的数量
     */
    static GameLoopFunction<Integer> schedule(final String cron) {
        return gameLoop -> gameLoop.getComponent(IScheduler.class)
                .map(iScheduleRegister -> iScheduleRegister.schedule(cron))
                .orElse(0);
    }

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IScheduler}注册cron调度
     *
     * @param object 要注册调度的实例
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功注册的方法的数量
     */
    static GameLoopFunction<Integer> registerSchedule(Object object) {
        return gameLoop -> gameLoop.getComponent(IScheduler.class)
                .map(iScheduleRegister -> iScheduleRegister.register(object))
                .orElse(0);
    }

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IScheduler}取消注册cron调度
     *
     * @param clazz 要取消注册的类
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功取消注册的方法的数量
     */
    static GameLoopFunction<Integer> unregisterSchedule(Class<?> clazz) {
        return gameLoop -> gameLoop.getComponent(IScheduler.class)
                .map(iScheduleRegister -> iScheduleRegister.unregister(clazz))
                .orElse(0);
    }
}
