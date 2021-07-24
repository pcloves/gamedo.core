package org.gamedo.gameloop.interfaces;

import org.gamedo.annotation.Cron;
import org.gamedo.annotation.Subscribe;
import org.gamedo.annotation.Tick;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

/**
 * <p>gamedo.core的线程模型借鉴了netty的线程模型：每一个{@link IGameLoop}实例代表一个线程，对应于Netty的EventLoop，而
 * {@link IGameLoopGroup}则对应于Netty的EventLoopGroup，正如Netty的EventLoopGroup一样，{@link IGameLoopGroup}虽然继承了
 * {@link ExecutorService}接口，但是自身只是一个{@link IGameLoop}容器，所有的功能则是由被轮询的{@link IGameLoop}提供，在实际的应用中，
 * 每个{@link IEntity}实例应该被唯一的安全发布到某个{@link IGameLoop}（安全发布是《JCP》一书中关于线程安全话题的重要概念，详情参考本圣经），
 * 这和Netty中某个Channel的整个生命周期都隶属于某一个线程（EventLoop）的理念也是一致的（这也是相对于Netty 3，Netty 4最大性能改进的设计理念）
 * 。<p>{@link IGameLoop}作为{@link IEntity}和{@link ScheduledExecutorService}的扩展，同时具备了两者的能力：异步（周期）执行任务和组
 * 件管理，除此之外，{@link IGameLoop}还提供了一个线程安全的，可以与之通信的能力：{@link IGameLoop#submit(GameLoopFunction)}，当提交线
 * 程不在本线程中时，任务被异步执行；否则就立即执行，此时可以通过{@link CompletableFuture#getNow(Object)}立刻得到结果，对于该接口的使用场
 * 景，主要包括以下几种：
 * <ul>
 * <li>明确不在本线程内：优先推荐使用本接口；其次考虑使用{@link ScheduledExecutorService}
 * <li>明确在本线程内：优先考虑先从{@link IEntity}中查询组件，然后使用组件提供的接口；其次考虑使用本接口
 * <li>不确定是否在本线程内：优先推荐使用本接口；其次考虑使用{@link ScheduledExecutorService}提供的接口
 * </ul>
 * 除了{@link IGameLoop#submit(GameLoopFunction)}，也可以通过{@link IGameLoopEventBus}与{@link IGameLoop}通信，例如：
 * <pre>
 *     final IGameLoop iGameLoop = ...
 *     final IEvent event = new SomeEvent();
 *     final CompletableFuture&lt;Integer&gt; future = iGameLoop.submit(IGameLoopEventBusFunction.post(event))
 * </pre>
 * <p>gamedo.core的starter工程利用spring boot的自动装配功能（autoconfigure），为{@link IGameLoop}自动装配了若干必备且开箱即用的组件，
 * 包括：
 * <ul>
 * <li> {@link IGameLoopEntityManager} 提供线程内的动态{@link IEntity}的管理机制
 * <li> {@link IGameLoopEventBus} 提供线程内的事件动态订阅、发布、处理机制
 * <li> {@link IGameLoopScheduler} 提供线程内的cron动态管理机制
 * <li> {@link IGameLoopTickManager} 提供线程内的逻辑心跳的动态管理机制
 * </ul>
 * <p>当某个{@link IEntity}实例被安全发布到{@link IGameLoop}上时，本实例及其所有组件就具备了事件订阅、cron调度、逻辑心跳的能力，详情可以参
 * 考{@link org.gamedo.annotation}包内关于{@link Subscribe}、{@link Cron}以及{@link Tick}的注释。这些组件的使用方式可以参考
 * {@link org.gamedo.gameloop.functions}包内提供的IGameLoop*Function函数或者单元测试，在实际开发过程中，可以通过类似的扩展机制对
 * {@link IGameLoop}的组件进行扩展，一般的实现流程为：
 * <ul>
 * <li> 实现IComponent&#60;IGameLoop&#62;，建议命名以“IGameLoop”作为前缀，这么做主要是为了和其他{@link IEntity}的组件做区分，例如某组
 * 件名为MyDemo，则命名为：IGameLoopMyDemo
 * <li> 在{@link org.gamedo.gameloop.functions}内创建IGameLoopMyDemoFunction类，并提供可复用的{@link GameLoopFunction}逻辑，当
 * 然，该步骤是可选的
 * </ul>
 * 如果没有使用starter项目，那么上述4个内置的{@link IGameLoop}组件，以及自定义扩展的组件，需要通过接口函数
 * {@link IEntity#addComponent(Class, Object)}手动添加
 */
public interface IGameLoop extends ScheduledExecutorService, IEntity {

    /**
     * 检测当前线程是否就是{@link IGameLoop}本线程
     * @return true 表示当前处于本GameLoop线程中
     */
    boolean inThread();

    /**
     * 提交一个操作到该{@link IGameLoop}，本函数是线程安全的，如果提交操作的线程就是{@link IGameLoop}本线程，则任务立即执行，可以通过
     * {@link CompletableFuture#getNow(Object)}立刻获得返回结果，否则就提交到{@link IGameLoop}上异步执行，对于该接口的使用场景，
     * 主要包括以下几种：
     * <ul>
     * <li>明确不在本线程内：优先推荐使用本接口；其次考虑使用{@link ScheduledExecutorService}提供的接口
     * <li>明确在本线程内：优先考虑先从{@link IEntity}中查询组件，然后使用组件提供的接口；其次考虑使用本接口
     * <li>不确定是否在本线程内：优先推荐使用本接口；其次考虑使用{@link ScheduledExecutorService}提供的接口
     * </ul>
     * 此外，无论是什么场景，都不建议调用返回值的{@link CompletableFuture#get()}或{@link CompletableFuture#join()}等阻塞函数，防止
     * 某些业务层上的bug阻塞当前线程，建议使用：{@link CompletableFuture#whenComplete(BiConsumer)}，并且如果{@link BiConsumer}逻
     * 辑较为复杂，建议使用函数引用，而非lambda表达式或匿名类，这两者在代码圈复杂度比较大时，不是那么优雅
     * 等
     * @param function 要提交的function
     * @param <R>      提交后的返回值类型
     * @return 操作返回结果
     */
    <R> CompletableFuture<R> submit(GameLoopFunction<R> function);
}
