package org.gamedo;

import org.gamedo.configuration.GameLoopGroupAutoConfiguration;
import org.gamedo.configuration.GameLoopProperties;
import org.gamedo.configuration.GamedoProperties;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.exception.GamedoException;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * {@link Gamedo}代表一个单例的应用类，它的作用就是将系统中各种单例的模块进行组合。上层应用需要继承该类，并根据实际需求采用<b>Holder</b>式的
 * 懒加载的机制实现{@link IGameLoopGroup}线程池的创建或者其他单例模块的集成，例如：
 * <pre>
 * //MyApp.java
 * &#64;Component //需要定义为spring bean
 * public class MyApp extends Gamedo
 * {
 *     protected MyApp(ApplicationContext applicationContext) {
 *         super(applicationContext);
 *     }
 *
 *     public static IGameLoopGroup db() {
 *         return HolderDb.db;
 *     }
 *
 *     public static GamedoMongoTemplate gamedoMongoTemplate() {
 *         return HolderGamedoMongoTemplate.mongoTemplate;
 *     }
 * }
 *
 * //HolderDb.java
 * &#64;SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization"})
 * private final class HolderDb
 * {
 *     //编程方式创建线程池，也可以采用父类Gamedo的方式，通过配置文件进行构造
 *     static final IGameLoopGroup db = applicationContext.getBean(IGameLoopGroup.class, GameLoopConfig.builder()
 *             .gameLoopGroupId("dbs")
 *             .nodeCountPerGameLoop(500)
 *             .gameLoopCount(10)
 *             .gameLoopIdPrefix("db-")
 *             .gameLoopImplClazz(GameLoop.class.getName())
 *             .componentRegisters(GameLoopConfig.DEFAULT.getComponentRegisters())
 *             .daemon(false)
 *             .build()
 *     );
 *
 *      private HolderDb() {
 *      }
 * }
 *
 * //HolderGamedoMongoTemplate.java
 * final class HolderGamedoMongoTemplate { //仅包可见
 *     //仅包可见
 *     static final GamedoMongoTemplate mongoTemplate = Dream.context().getBean(GamedoMongoTemplate.class);
 *
 *     //封闭构造函数，防止外界随意实例化
 *     private HolderGamedoMongoTemplate() {
 *     }
 * }
 * </pre>
 * <p>
 * 有如下几点需要注意：
 * <ul>
 * <li> {@link Gamedo}及其子类都应该作为单例的bean而存在，不应该存在多份，否则线程池将会被重复创建
 * <li> {@link Gamedo}同时还是一个{@link IEntity}实体，因此可以在构造函数中为其指定组件map，或者通过{@link Gamedo#instance()}为其注册
 * 组件或者删除组件
 * <li> {@link Gamedo}的子类也可以同时implements其他接口，扩展自身的功能
 * <li> 假如应用层没有实现{@link Gamedo}的子类，那么{@link GameLoopGroupAutoConfiguration}会自动装配一个{@link Gamedo}单例
 * <li> {@link Gamedo}采用懒加载机制，当且仅当第一次调用诸如{@link Gamedo#io()}静态方法时，{@link HolderIo#io}线程池才会被创建
 * <li> 在调用{@link Gamedo}及其子类的构造函数前，不要调用{@link HolderIo#io}、{@link HolderSingle#single}、{@link HolderWorker}
 * （例如在IDE中添加debug监控），否则会导致构造失败
 * </ul>
 */
@SuppressWarnings("unused")
public abstract class Gamedo extends Entity {
    private static final AtomicReference<Gamedo> GAMEDO_ATOMIC_REFERENCE = new AtomicReference<>(null);

    protected static ApplicationContext applicationContext;
    protected static GameLoopProperties gameLoopProperties;
    protected static GamedoProperties gamedoProperties;

    protected Gamedo(ApplicationContext applicationContext) {
        super(applicationContext.getBean(GamedoProperties.class).getName());

        construct(applicationContext);
    }

    protected Gamedo(ApplicationContext applicationContext, String id) {
        super(id);

        construct(applicationContext);
    }

    protected Gamedo(ApplicationContext applicationContext, Supplier<String> idSupplier) {
        super(idSupplier);

        construct(applicationContext);
    }

    protected Gamedo(ApplicationContext applicationContext, Map<Class<?>, Object> componentMap) {
        super(applicationContext.getBean(GamedoProperties.class).getName(), componentMap);

        construct(applicationContext);
    }

    protected Gamedo(ApplicationContext applicationContext, String id, Map<Class<?>, Object> componentMap) {
        super(id, componentMap);

        construct(applicationContext);
    }

    private void construct(ApplicationContext applicationContext) {
        if (!GAMEDO_ATOMIC_REFERENCE.compareAndSet(null, this)) {
            throw new GamedoException("multi " + getClass().getSimpleName() + " instantiated.");
        }

        Gamedo.applicationContext = applicationContext;
        gameLoopProperties = applicationContext.getBean(GameLoopProperties.class);
        gamedoProperties = applicationContext.getBean(GamedoProperties.class);
    }

    public static Gamedo instance() {
        return GAMEDO_ATOMIC_REFERENCE.get();
    }

    /**
     * spring上下文，可以获得spring bean或者自定义的{@link IGameLoopGroup}线程池，例如：
     * <pre>
     *     GameLoopConfig config = ...
     *     IGameLoopGroup gameLoopGroup = Gamedo.context().getBean(IGameLoopGroup.class, config);
     * </pre>
     * 注意：由于{@link IGameLoopGroup}的scope为prototype，因此每次调用都会产生新的{@link IGameLoopGroup}，使用者需要自己缓存起来！
     *
     * @return spring 上下文
     */
    public static ApplicationContext context() {
        return applicationContext;
    }

    /**
     * 返回cpu密集型线程池，该线程池类似于RxJava的Schedulers.computation()或Reactor的Schedulers.parallel()，线程池内默认线程数量为：
     * {@link Runtime#availableProcessors()} + 1
     *
     * @return 计算型线程池
     */
    public static IGameLoopGroup worker() {
        return HolderWorker.worker;
    }

    /**
     * 返回io密集型线程池，该线程池类似于RxJava的Scheduler.io()或Reactor的Schedulers.boundedElastic（），线程池内默认线程数量为：
     * {@link Runtime#availableProcessors()} * 10，在实际应用中，这个值应该是根据分析或者监控工具进行指标检测，然后根据公式计算得出，
     * 《JCP》建议通过估算任务等待时间和计算时间的比值，来估算io密集型的线程数量，并给出了确切计算方案。<p>
     * 首先给出下列定义：
     * <pre>
     *      N(cpu) = CPU的数量，也即Runtime.getRuntime().availableProcessors()
     *      U(cpu) = CPU利用率，且0 &lt;= U(cpu) &lt;= 1
     *      W/C    = 等待时间和计算时间的比率
     *     </pre>
     * 如果要使得达到期望的利用率，那么io线程池的最优大小等于：
     * <pre>
     *          N(threads) = N(cpu) * U(cpu) * (1 + W/C)
     *     </pre>
     *
     * @return io密集型线程池
     */
    public static IGameLoopGroup io() {
        return HolderIo.io;
    }

    /**
     * 返回一个单线程的线程池，某些并发业务场景需要操作强一致性（例如经典的抢票行为），对于这种需求，可以将请求提交到本线程池，使得并行请求串行化，
     * 解决并发业务带来的复杂性
     *
     * @return 单线程线程池
     */
    public static IGameLoopGroup single() {
        return HolderSingle.single;
    }
}
