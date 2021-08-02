package org.gamedo;

import org.gamedo.configuration.GameLoopProperties;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public final class Gamedo {
    private static IGameLoopGroup worker;
    private static IGameLoopGroup io;
    private static IGameLoopGroup single;
    private static ApplicationContext applicationContext;

    @Autowired
    private Gamedo(ApplicationContext applicationContext, GameLoopProperties gameLoopProperties) {
        Gamedo.applicationContext = applicationContext;

        worker = applicationContext.getBean(IGameLoopGroup.class, gameLoopProperties.getWorkers().convert());
        io = applicationContext.getBean(IGameLoopGroup.class, gameLoopProperties.getIos().convert());
        single = applicationContext.getBean(IGameLoopGroup.class, gameLoopProperties.getSingles().convert());
    }

    private Gamedo() {
    }

    /**
     * spring上下文，可以获得spring bean或者自定义的{@link IGameLoopGroup}线程池，例如：
     * <pre>
     *     GameLoopConfig config = ...
     *     IGameLoopGroup gameLoopGroup = Gamedo.context().getBean(IGameLoopGroup.class, config);
     * </pre>
     * 注意：由于{@link IGameLoopGroup}的scope为prototype，因此每次调用都会产生新的{@link IGameLoopGroup}，使用者需要自己缓存起来！
     * @return spring 上下文
     */
    public static ApplicationContext context() {
        return applicationContext;
    }

    /**
     * 返回cpu密集型线程池，该线程池类似于RxJava的Schedulers.computation()或Reactor的Schedulers.parallel()，线程池内默认线程数量为：
     * {@link Runtime#availableProcessors()} + 1
     * @return 计算型线程池
     */
    public static IGameLoopGroup worker() {
        return worker;
    }

    /**
     * 返回io密集型线程池，该线程池类似于RxJava的Scheduler.io()或Reactor的Schedulers.boundedElastic（），线程池内默认线程数量为：
     * {@link Runtime#availableProcessors()} * 10，在实际应用中，这个值应该是根据分析或者监控工具进行指标检测，然后根据公式计算得出，在
     * 《JCP》一书中，建议通过估算任务等待时间和计算时间的比值，来估算io密集型的线程数量，并给出了确切的计算方案。<p>
     *     首先给出下列定义：
     *     <pre>
     *      N(cpu) = CPU的数量，也即Runtime.getRuntime().availableProcessors()
     *      U(cpu) = CPU利用率，且0 &lt;= U(cpu) &lt;= 1
     *      W/C    = 等待时间和计算时间的比率
     *     </pre>
     *     如果要使得达到期望的利用率，那么io线程池的最优大小等于：
     *     <pre>
 *          N(threads) = N(cpu) * U(cpu) * (1 + W/C)
     *     </pre>
     * @return io密集型线程池
     */
    public static IGameLoopGroup io() {
        return io;
    }

    /**
     * 返回一个单线程的线程池，某些并发业务场景需要操作强一致性（例如经典的抢票行为），对于这种需求，可以将请求提交到本线程池，是的并行请求串行化，
     * 解决并发业务带来的复杂性
     * 线程池内
     * @return 单线程线程池
     */
    public static IGameLoopGroup single() {
        return single;
    }
}
