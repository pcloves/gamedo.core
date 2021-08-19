package org.gamedo;

import org.gamedo.configuration.GameLoopGroupAutoConfiguration;
import org.gamedo.configuration.GameLoopProperties;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.context.ApplicationContext;

/**
 * 默认的线程池应用类，上层应用需要继承该类，并根据实际需求实现类似的{@link IGameLoopGroup}，例如：
 * <pre>
 * &#64;Component
 * public class MyApp extends Gamedo
 * {
 *     &#64;SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization"})
 *     private static final class MyHolder
 *     {
 *         //代码式创建线程池，也可以根据外部配置文件创建线程池
 *         public static final IGameLoopGroup db = applicationContext.getBean(IGameLoopGroup.class, GameLoopConfig.builder()
 *                 .gameLoopGroupId("dbs")
 *                 .gameLoopCount(10)
 *                 .gameLoopIdPrefix("db-")
 *                 .componentRegisters(GameLoopConfig.DEFAULT.getComponentRegisters())
 *                 .daemon(false)
 *                 .build()
 *         );
 *     }
 *
 *     protected MyApp(ApplicationContext applicationContext, GameLoopProperties gameLoopProperties) {
 *         super(applicationContext, gameLoopProperties);
 *     }
 *
 *     public static IGameLoopGroup db() {
 *         return MyHolder.db;
 *     }
 * }
 * </pre>
 * 有如下几点需要注意：
 * <ul>
 * <li> {@link Gamedo}及其子类都应该作为单例的bean而存在，不应该存在多份，否则线程池将会被重复创建
 * <li> 假如应用层没有实现{@link Gamedo}的子类，那么{@link GameLoopGroupAutoConfiguration}会自动装配一个{@link Gamedo}单例
 * <li> {@link Gamedo}采用懒加载机制，当且仅当第一次调用诸如{@link Gamedo#io()}静态方法时，{@link HolderIo#io}线程池才会被创建
 * <li> 在调用{@link Gamedo}及其子类的构造函数前，不要调用{@link HolderIo#io}、{@link HolderSingle#single}、
 * {@link HolderWorker#worker}（例如在IDE中watch这），否则会导致构造失败
 * </ul>
 */
@SuppressWarnings("unused")
public abstract class Gamedo {
    protected static ApplicationContext applicationContext;
    protected static GameLoopProperties gameLoopProperties;

    /**
     * 实现延迟加载，当且仅当{@link Gamedo#worker() Gamedo.worker()}被调用时，{@link HolderWorker#worker}线程组才会被初始化，并且由
     * jvm的class lock确保线程安全
     */
    @SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
    private static class HolderWorker
    {
        private static final IGameLoopGroup worker = applicationContext.getBean(IGameLoopGroup.class, gameLoopProperties.getWorkers().convert());
    }

    /**
     * 实现延迟加载，当且仅当{@link Gamedo#io() Gamedo.io()}被调用时，{@link HolderIo#io}线程组才会被初始化，并且由jvm的class lock
     * 确保线程安全
     */
    @SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
    private static class HolderIo
    {
        private static final IGameLoopGroup io = applicationContext.getBean(IGameLoopGroup.class, gameLoopProperties.getIos().convert());
    }

    /**
     * 实现延迟加载，当且仅当{@link Gamedo#single() Gamedo.single}被调用时，{@link HolderSingle#single}线程组才会被初始化，并且由
     * jvm的class lock确保线程安全
     */
    @SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
    private static class HolderSingle
    {
        private static final IGameLoopGroup single = applicationContext.getBean(IGameLoopGroup.class, gameLoopProperties.getSingles().convert());
    }

    protected Gamedo(ApplicationContext applicationContext, GameLoopProperties gameLoopProperties) {
        Gamedo.applicationContext = applicationContext;
        Gamedo.gameLoopProperties = gameLoopProperties;
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
        return HolderWorker.worker;
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
        return HolderIo.io;
    }

    /**
     * 返回一个单线程的线程池，某些并发业务场景需要操作强一致性（例如经典的抢票行为），对于这种需求，可以将请求提交到本线程池，是的并行请求串行化，
     * 解决并发业务带来的复杂性
     * 线程池内
     * @return 单线程线程池
     */
    public static IGameLoopGroup single() {
        return HolderSingle.single;
    }
}
