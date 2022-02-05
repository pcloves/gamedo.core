package org.gamedo;

import lombok.extern.log4j.Log4j2;
import org.gamedo.event.EventGameLoopCreatePost;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * {@link ApplicationBase}是一个静态工具类，提供便捷的线程相关的静态函数，需要注意的是：当调用该类的静态函数时，需要确保其构造函数至
 * 少被执行过1次，可以通过如下3种方式来保证：
 * <ul>
 * <li> 将main函数所在的类继承自该类，由于main函数类的实例化时机相对较早，因此可以确保本类构造函数能在很早的时期被调用
 * <li> 调用bean使用{@link DependsOn}注解进行依赖，例如：@DependsOn(ApplicationBase.BeanName)
 * <li> 如同第1种方式，直接将 @DependsOn(ApplicationBase.BeanName) 注解加在main函数类上（推荐）
 * </ul>
 */
@SuppressWarnings("unused")
@Log4j2
@Component(ApplicationBase.BeanName)
public class ApplicationBase {

    public static final String BeanName = "ApplicationBase";

    private static final AtomicBoolean init = new AtomicBoolean(false);

    protected static ApplicationContext applicationContext;

    private static IGameLoopGroup workers;
    private static IGameLoopGroup singles;

    private static IGameLoop worker;
    private static IGameLoop single;

    /**
     * 本构造函数是可重入的，因此可以用来当做父类
     *
     * @param applicationContext 上下文容器
     */
    protected ApplicationBase(ConfigurableApplicationContext applicationContext) {

        if (init.compareAndSet(false, true)) {

            ApplicationBase.applicationContext = applicationContext;

            ApplicationBase.workers = applicationContext.getBean("worker", IGameLoopGroup.class);
            ApplicationBase.worker = workers.selectAll()[0];

            ApplicationBase.singles = applicationContext.getBean("single", IGameLoopGroup.class);
            ApplicationBase.single = workers.selectAll()[0];

            applicationContext.registerShutdownHook();
        }
    }

    /**
     * spring上下文
     *
     * @return spring 上下文
     */
    public static ApplicationContext context() {
        return applicationContext;
    }

    /**
     * 返回worker线程，且默认线程名为：“worker-1”，如果上层业务主逻辑都在同一个线程中调用，那么可以使用该线程作为主逻辑线程
     *
     * @return 计算型线程池
     */
    public static IGameLoop worker() {
        return worker;
    }

    /**
     * 返回single线程，且默认线程名为：“single-1”，如果上层业务的操作需要执行一些cpu密集型的操作，可以在该线程中
     *
     * @return 单线程线程池
     */
    public static IGameLoop single() {
        return single;
    }

    /**
     * 创建一个线程池
     *
     * @param config  线程池配置
     * @param context 上下文容器
     * @return 返回一个新创建的线程池
     */
    public static IGameLoopGroup createGameLoopGroup(GameLoopConfig config, ApplicationContext context) {

        final String gameLoopIdPrefix = config.getGameLoopIdPrefix();
        final IntFunction<IGameLoop> gameLoopIntFunction = i -> context.getBean(IGameLoop.class, config);

        final IGameLoop[] iGameLoops = IntStream.rangeClosed(1, config.getGameLoopCount())
                .mapToObj(gameLoopIntFunction)
                .toArray(IGameLoop[]::new);

        final IGameLoopGroup gameLoopGroup = new GameLoopGroup(config.getGameLoopGroupId(), config.getNodeCountPerGameLoop(), iGameLoops);

        Arrays.stream(gameLoopGroup.selectAll())
                .peek(gameLoop -> ((GameLoop) gameLoop).setOwner(gameLoopGroup))
                .peek(gameLoop -> gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(gameLoop)))
                .forEach(gameLoop -> gameLoop.submit(IGameLoopEventBusFunction.post(new EventGameLoopCreatePost(gameLoop))));

        return gameLoopGroup;
    }
}
