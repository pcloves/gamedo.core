package org.gamedo.gameloop.interfaces;

import org.gamedo.annotation.Cron;
import org.gamedo.annotation.Subscribe;
import org.gamedo.annotation.Tick;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.util.function.EntityFunction;
import org.gamedo.util.function.GameLoopFunction;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;

import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * <p>gamedo.core的线程模型借鉴了netty的线程模型：每一个{@link IGameLoop}实例代表一个线程，对应于Netty的EventLoop，而
 * {@link IGameLoopGroup}则对应于Netty的EventLoopGroup，正如Netty的EventLoopGroup一样，{@link IGameLoopGroup}虽然继承了
 * {@link ExecutorService}接口，但是自身只是一个{@link IGameLoop}容器，所有的功能则是通过轮询（round robin）的{@link IGameLoop}提供，
 * 在实际的应用中，每个{@link IEntity}实例应该被唯一的安全发布（safe publication）到某个{@link IGameLoop}（安全发布是《JCP》一书中关于
 * 线程安全话题的重要概念，详情可以查阅本书），这和Netty中任意Channel的整个生命周期都隶属于某一个线程（EventLoop）的理念也是一致的（这也是相对
 * 于Netty 3，Netty 4为什么能获得巨大性能提升的重要原因之一）。
 * <p>{@link IGameLoop}作为{@link IEntity}和{@link ScheduledExecutorService}的扩展，同时具备了两者的能力：异步（延迟、周期）执行任务
 * 和组件管理，除此之外，{@link IGameLoop}还提供了一个线程安全的，可以与之通信的能力：{@link IGameLoop#submit(EntityFunction)}，和
 * {@link ScheduledExecutorService}不同的是：当提交线程不在本线程中时，任务被异步执行；当提交线程就在本线程内时，任务会同步立即执行，此时调
 * 用者可以通过{@link CompletableFuture#getNow(Object)}立刻得到结果，对于该函数的使用场景，主要包括以下几种：
 * <ul>
 * <li> 明确不在本线程内：优先推荐使用本函数；例如：{@link IGameLoopEventBusFunction#post(Class, Supplier)} )}，其次考虑使用
 * {@link ScheduledExecutorService}提供的异步函数或异步延迟函数
 * <li> 明确在本线程内：优先考虑先通过{@link IEntity}接口查询组件，然后使用相应的组件提供的函数；其次考虑使用本函数
 * <li> 不确定是否在本线程内：优先推荐使用本函数；其次考虑使用{@link ScheduledExecutorService}提供的异步函数或异步延迟执行函数
 * </ul>
 * {@link IGameLoop#submit(EntityFunction)}提供了线程安全的，由外部世界发起的，将任意数据类型X提交到{@link IGameLoop}线程，并且可以
 * 返回任意类型Y的双向通信能力，除此之外，{@link IGameLoop}的内置组件：{@link IGameLoopEventBus}也提供了外界与{@link IGameLoop}单向通
 * 信的能力，例如：
 * <pre>
 *     final IGameLoop iGameLoop = ...
 *     final IEvent event = new SomeEvent();
 *     final CompletableFuture&lt;Integer&gt; future = iGameLoop.submit(IGameLoopEventBusFunction.post(event))
 * </pre>
 * <p>gamedo.core的starter工程利用spring boot的自动装配功能（autoconfigure），为{@link IGameLoop}自动装配了若干必备且开箱即用的组件，
 * 包括：
 * <ul>
 * <li> {@link IGameLoopEntityManager} 提供线程内的{@link IEntity}管理机制
 * <li> {@link IGameLoopEventBus} 提供线程内的事件动态订阅、发布、处理机制
 * <li> {@link IGameLoopScheduler} 提供线程内的cron动态管理机制
 * <li> {@link IGameLoopTickManager} 提供线程内的逻辑心跳的动态管理机制
 * </ul>
 * <p>当某个{@link IEntity}实例被安全发布到{@link IGameLoop}上时，本实例及其所有组件都具备了事件订阅、cron延迟运行、逻辑心跳的能力，详情可
 * 以参考{@link org.gamedo.annotation}包内关于{@link Subscribe}、{@link Cron}以及{@link Tick}的注释。这些组件的使用方式可以参考
 * {@link org.gamedo.util.function}包内提供的IGameLoop*Function函数或者单元测试，在实际开发过程中，可以通过类似的扩展机制对
 * {@link IGameLoop}的组件进行扩展，一般实现流程为：
 * <ul>
 * <li> 定义待扩展组件自己的接口，同时要求extends {@link IComponent}，并建议命名以“IGameLoop”作为前缀，这么做主要是为了和{@link IEntity}
 * 的组件做区分，例如某组件名为MyDemo，则命名为：IGameLoopMyDemo，可以参考org.gamedo.gameloop.components包内任意组件的接口定义
 * <li> （可选）创建自己的{@link GameLoopFunction} helper类：IGameLoopMyDemoFunction类，提供可复用的{@link GameLoopFunction}
 * 逻辑，例如：{@link IGameLoopEventBusFunction}
 * <li> 通过{@link GameLoop#GameLoop(GameLoopConfig)}实例化{@link IGameLoop}
 * <li> 或者通过{@link GameLoop#GameLoop(GameLoopConfig)}实例化{@link IGameLoop}，该构造函数和上一
 * 步的区别在于{@link GameLoopConfig}内的所有{@link GameLoopComponent}组件从spring容器获取，可以参考autoconfigure工程的
 * GameLoopGroupAutoConfiguration类自动装配实践
 * </ul>
 * Tips:
 * <ul>
 * <li> 当{@link IGameLoop}被实例化之后，也可以通过{@link IGameLoopEntityManagerFunction#registerEntity(IEntity)}函数注册自己，
 * 使得{@link IGameLoop}自身及其组件也具备事件订阅、cron延迟运行、逻辑心跳的基础能力，例如：
 * <pre>
 *     final GameLoop gameLoop = new GameLoop(config);
 *     gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(gameLoop));
 * </pre>
 * <li> {@link IGameLoop}作为{@link ScheduledExecutorService}的子接口，自身又是一个独立的线程，因此需要确保对其访问都是线程安全的，为
 * 此，在其默认实现类：{@link GameLoop}的实现中，增加了线程检测的判定：如果在外部线程调用{@link IEntity}下除{@link IEntity#getId()}之外
 * 的任何函数，都会抛出{@link GameLoopException}，如有必要，请使用线程安全的通信方式，例如在运行时期间，动态注册一个组件：
 * <pre>
 *     gameLoop.submit(gameLoop -&#62; gameLoop.addComponent(SomeInterface.class, new SomeImplementation()));
 * </pre>
 * 然而，需要记住的是：不要以把那些被{@link IGameLoop}线程管理的对象发布到外部线程（除非这是一个
 * <a href=https://en.wikipedia.org/wiki/Immutable_object>不可变对象</a>），实际上不可变对象天然是线程安全的，因此也不必要多此一举：
 * 事先线程安全地将其发布到{@link IGameLoop}中），这会带来包括内存可见性(memory visibility)、竞态条件（race condition）在内的多线程并
 * 发问题，例如：
 * <pre>
 *     gameLoop.submit(gameLoop -&#62; gameLoop.getComponentMap())
 *                 .thenAccept(map -&#62; log.info("{}", map));
 * </pre>
 * 在本例中，虽然可以将componentMap线程安全地从{@link IGameLoop}发布到调用线程，然而由于componentMap仍然还存在于{@link IGameLoop}的管
 * 理之下，并且随时可能对componentMap或者componentMap管理的的Object进行写操作，java内存模型（JMM）并不能保证其写操作对其他线程可见
 * (happens before)，这就带来的内存可见性问题。此外，如果{@link IGameLoop}线程对componentMap进行了put/remove操作，于此同时，调用线程对
 * map进行了遍历操作，这会导致调用线程抛出{@link ConcurrentModificationException}，这虽然不属于竞态条件的问题，但是其中的风险可想而知。
 * </ul>
 */
public interface IGameLoop extends ScheduledExecutorService, IEntity {

    /**
     * 检测当前线程是否就是{@link IGameLoop}本线程，在如下情况中，会返回true
     * <ul>
     * <li> 当调用{@link IGameLoop#submit(EntityFunction)}提交任务后，如果在任务执行函数内调用本函数
     * <li> 当调用{@link ScheduledExecutorService}}接口的任意函数提交异步任务或异步延迟任务后，如果在任务的执行函数
     * （{@link Callable#call()}、{@link Runnable#run()}）里调用本函数
     * <li> 在{@link Cron}、{@link Tick}、{@link Subscribe}注解函数里调用本函数
     * </ul>
     *
     * @return true 表示当前处于本GameLoop线程中
     */
    boolean inThread();

    /**
     * 所归属的{@link IGameLoopGroup}
     * @return 如果尚未注册，则返回Optional.empty()
     */
    Optional<IGameLoopGroup> owner();

    /**
     * 提交一个操作到该{@link IGameLoop}，本函数是线程安全的，如果提交操作的线程就是{@link IGameLoop}本线程，则任务立即执行，可以通过
     * {@link CompletableFuture#getNow(Object)}立刻获得返回结果，否则就提交到{@link IGameLoop}上异步执行，对于该接口的使用场景，
     * 主要包括以下几种：
     * <ul>
     * <li> 明确不在本线程内：优先推荐使用本函数；例如：{@link IGameLoopEventBusFunction#post(Class, Supplier)} )}，其次考虑使用
     * {@link ScheduledExecutorService}提供的异步函数或异步延迟函数
     * <li> 明确在本线程内：优先考虑先通过{@link IEntity}接口查询组件，然后使用相应的组件提供的函数；其次考虑使用本函数
     * <li> 不确定是否在本线程内：优先推荐使用本函数；其次考虑使用{@link ScheduledExecutorService}提供的异步函数或异步延迟执行函数
     * </ul>
     * 此外，不建议调用返回值的{@link CompletableFuture#get()}或{@link CompletableFuture#join()}等阻塞函数，防止某些业务层上的bug
     * 阻塞调用线程，建议使用：{@link CompletableFuture#whenComplete(BiConsumer)}，如果{@link BiConsumer}逻辑较为复杂，建议将逻辑包
     * 装为函数，并使用函数引用，而非lambda表达式或匿名类，这两者在代码圈复杂度比较大时，不是那么优雅，并且异常处理也相对比较复杂
     *
     * @param function 要提交的function
     * @param <R>      提交后的返回值类型
     * @return 操作返回结果
     */
    <R> CompletableFuture<R> submit(EntityFunction<IGameLoop, R> function);

    @Override
    default String getCategory() {
        return "GameLoop";
    }
}
