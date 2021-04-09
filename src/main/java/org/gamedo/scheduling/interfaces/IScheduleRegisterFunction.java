package org.gamedo.scheduling.interfaces;

import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

public interface IScheduleRegisterFunction {

    /**
     * 定义一个行为：向{@link IScheduleRegister}执行一次cron调度
     *
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功调度的方法的数量
     */
    static GameLoopFunction<Integer> schedule(final String cron) {
        return gameLoop -> gameLoop.getComponent(IScheduleRegister.class)
                .map(iScheduleRegister -> iScheduleRegister.schedule(cron))
                .orElse(0);
    }

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IScheduleRegister}组件注册一个组件
     *
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功调度的方法的数量
     */
    static GameLoopFunction<Integer> registerSchedule(Object component) {
        return gameLoop -> gameLoop.getComponent(IScheduleRegister.class)
                .map(iScheduleRegister -> iScheduleRegister.register(component))
                .orElse(0);
    }
}
