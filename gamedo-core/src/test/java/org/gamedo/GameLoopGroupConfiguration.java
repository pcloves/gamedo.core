package org.gamedo;

import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopComponentRegister;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.event.EventGameLoopCreatePost;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.GameLoopScheduler;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.GameLoopTickManager;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = GameLoopGroup.class)
public class GameLoopGroupConfiguration {

    private final ApplicationContext context;

    public GameLoopGroupConfiguration(ApplicationContext context) {
        this.context = context;
    }

    @Bean(name = "gameLoopConfig")
    @ConditionalOnMissingBean(value = GameLoopConfig.class, name = "gameLoopConfig")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    GameLoopConfig gameLoopConfig() {
        return GameLoopConfig.builder()
                .gameLoopGroupId("defaults")
                .nodeCountPerGameLoop(500)
                .gameLoopIdPrefix("default-")
                .gameLoopIdCounter(new AtomicInteger(1))
                .daemon(true)
                .gameLoopImplClazz(GameLoop.class)
                .gameLoopCount(Runtime.getRuntime().availableProcessors())
                .componentRegister(GameLoopComponentRegister.builder()
                        .allInterface(IGameLoopEntityManager.class)
                        .implementation(GameLoopEntityManager.class)
                        .build())
                .componentRegister(GameLoopComponentRegister.builder()
                        .allInterface(IGameLoopEventBus.class)
                        .implementation(GameLoopEventBus.class)
                        .build())
                .componentRegister(GameLoopComponentRegister.builder()
                        .allInterface(IGameLoopScheduler.class)
                        .implementation(GameLoopScheduler.class)
                        .build())
                .componentRegister(GameLoopComponentRegister.builder()
                        .allInterface(IGameLoopTickManager.class)
                        .implementation(GameLoopTickManager.class)
                        .build())
                .build();
    }

    @Bean(name = "gameLoop")
    @ConditionalOnMissingBean(value = IGameLoop.class, name = "gameLoop")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoop gameLoop(GameLoopConfig config) throws NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {

        final Class<? extends IGameLoop> gameLoopClazz = config.getGameLoopImplClazz();
        return gameLoopClazz.getConstructor(GameLoopConfig.class).newInstance(config);
    }

    @Bean(name = "gameLoopGroup")
    @ConditionalOnMissingBean(value = IGameLoopGroup.class, name = "gameLoopGroup")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoopGroup gameLoopGroup(GameLoopConfig config) {

        final IGameLoop[] iGameLoops = IntStream.rangeClosed(1, config.getGameLoopCount())
                .mapToObj(i -> context.getBean(IGameLoop.class, config))
                .toArray(IGameLoop[]::new);

        final IGameLoopGroup gameLoopGroup = new GameLoopGroup(config.getGameLoopGroupId(), config.getNodeCountPerGameLoop(), iGameLoops);

        Arrays.stream(gameLoopGroup.selectAll())
                .peek(gameLoop -> ((GameLoop) gameLoop).setOwner(gameLoopGroup))
                .peek(gameLoop -> gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(gameLoop)))
                .forEach(gameLoop -> gameLoop.submit(IGameLoopEventBusFunction.post(new EventGameLoopCreatePost(gameLoop))));

        return gameLoopGroup;
    }
}
