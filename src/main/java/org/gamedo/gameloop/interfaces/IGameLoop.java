package org.gamedo.gameloop.interfaces;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IEntityManagerFunction;
import org.gamedo.ecs.interfaces.IGameLoopEntityRegister;
import org.gamedo.eventbus.interfaces.IEvent;
import org.gamedo.eventbus.interfaces.IEventBusFunction;
import org.gamedo.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.scheduling.interfaces.ISchedulerFunction;
import org.gamedo.timer.ITimer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p>gamedo.core的线程模型借鉴了netty 4的线程模型设计，每一个IGameLoop代表一个线程，对应于Netty 4的EventLoop，每一个{@link IGameLoop}
 * 又是一个{@link IEntity}，代表着它可以组合很多的{@link IComponent}，从而具备无限的逻辑扩展能力。
 *
 * <p>gamedo.core已经为{@link IGameLoop}装配了若干开箱即用的组件，包括：
 *
 * <ul>
 * <li> {@link IGameLoopEventBus} 提供事件处理机制
 * <li> {@link IGameLoopEntityRegister} 提供{@link IEntity}的管理机制，可以使用{@link IEntityManagerFunction#registerEntity(IEntity)}
 * 实现将某个实体注册到该{@link IGameLoop}中
 * <li> {@link ITimer} 提供线程内的延迟和定时回调机制，
 * <li> {@link IGameLoopScheduler} 提供线程内的cron调度策略，可以使用{@link ISchedulerFunction#registerSchedule(Object)}
 * 实现某个Object在本{@link IGameLoop}上的cron调度
 * </ul>
 */
public interface IGameLoop extends ScheduledExecutorService, IEntity {

    ThreadLocal<Optional<IGameLoop>> GAME_LOOP_THREAD_LOCAL = ThreadLocal.withInitial(Optional::empty);

    /**
     * 返回当前的{@link IGameLoop}，当且仅当该方法在某个{@link IGameLoop}线程内调用时，返回所属的{@link IGameLoop}，否则返回
     * {@link Optional#empty()}
     * @return 当前锁归属的IGameLoop
     */
    static Optional<IGameLoop> currentGameLoop() {
        return GAME_LOOP_THREAD_LOCAL.get();
    }

    /**
     * @return true 表示当前处于本GameLoop线程中
     */
    boolean inGameLoop();

    /**
     * 启动该线程
     *
     * @param initialDelay   初始延迟
     * @param period         心跳间隔
     * @param periodTimeUnit 心跳间隔的单位
     * @return 当且仅当启动成功时返回true，如果之前已经启动，则启动失败
     */
    boolean run(long initialDelay, long period, TimeUnit periodTimeUnit);

    /**
     * 提交一个异步操作到该{@link IGameLoop}
     *
     * @param function 要提交的function
     * @param <R>      提交后的返回值类型
     * @return 操作返回结果
     */
    <R> CompletableFuture<R> submit(GameLoopFunction<R> function);

    /**
     * 提交一个时间到该{@link IGameLoop}
     *
     * @param event 要提交的事件
     * @return 该事件被成功（如果抛出异常，就不算成功）消费的个数
     */
    default CompletableFuture<Integer> postEvent(IEvent event) {
        return submit(IEventBusFunction.post(event));
    }
}
