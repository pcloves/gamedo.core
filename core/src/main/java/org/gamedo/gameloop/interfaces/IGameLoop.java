package org.gamedo.gameloop.interfaces;

import org.gamedo.annotation.Scheduled;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBusFunction;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopSchedulerFunction;
import org.gamedo.timer.ITimer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * <p>gamedo.core的线程模型借鉴了netty 4的线程模型设计，每一个IGameLoop代表一个线程，对应于Netty 4的EventLoop，每一个{@link IGameLoop}
 * 又是一个{@link IEntity}，代表着它可以组合很多的{@link IComponent}，从而具备无限的逻辑扩展能力。
 *
 * <p>gamedo.core已经为{@link IGameLoop}装配了若干开箱即用的组件，包括：
 *
 * <ul>
 * <li> {@link IGameLoopEventBus} 提供事件订阅、发布、处理机制
 * <li> {@link IGameLoopEntityManager} 提供{@link IEntity}的管理机制，对于外部线程，可以使用{@link IGameLoopEntityManagerFunction#registerEntity(IEntity)}
 * 实现将某个实体注册到该{@link IGameLoop}中
 * <li> {@link ITimer} 提供线程内毫秒级的延迟和定时回调机制
 * <li> {@link IGameLoopScheduler} 提供线程内的cron调度机制，对于外部线程，可以使用{@link IGameLoopSchedulerFunction#register(Object)}
 * 实现某个Object在本{@link IGameLoop}上的spring cron定时调度，详情参考{@link Scheduled}
 * </ul>
 */
public interface IGameLoop extends ScheduledExecutorService, IEntity {

    ThreadLocal<Optional<IGameLoop>> GAME_LOOP_THREAD_LOCAL = ThreadLocal.withInitial(Optional::empty);

    /**
     * 返回当前的{@link IGameLoop}，当且仅当调用方法在某个{@link IGameLoop}线程的调用栈内时，返回所属的{@link IGameLoop}，否则返回
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
        return submit(IGameLoopEventBusFunction.post(event));
    }
}
