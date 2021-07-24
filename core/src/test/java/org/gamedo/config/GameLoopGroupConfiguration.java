package org.gamedo.config;

import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopComponentRegister;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.event.EventGameLoopCreatePost;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.GameLoopScheduler;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.GameLoopTickManager;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.functions.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.functions.IGameLoopEventBusFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Configuration(proxyBeanMethods = false)
public class GameLoopGroupConfiguration {

    private static final AtomicInteger gameLoopCounter = new AtomicInteger(1);
    private final ApplicationContext context;

    public GameLoopGroupConfiguration(ApplicationContext context) {
        this.context = context;
    }

    @Bean(name = "defaultGameLoopConfig")
    @ConditionalOnMissingBean(value = GameLoopConfig.class, name = "defaultGameLoopConfig")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    GameLoopConfig gameLoopConfig() {
        return GameLoopConfig.builder().id("default-" + gameLoopCounter.getAndIncrement())
                .componentRegister(GameLoopComponentRegister.<GameLoopEntityManager>builder()
                        .interfaceClazz(IGameLoopEntityManager.class)
                        .componentClazz(GameLoopEntityManager.class)
                        .build())
                .componentRegister(GameLoopComponentRegister.<GameLoopEventBus>builder()
                        .interfaceClazz(IGameLoopEventBus.class)
                        .componentClazz(GameLoopEventBus.class)
                        .build())
                .componentRegister(GameLoopComponentRegister.<GameLoopScheduler>builder()
                        .interfaceClazz(IGameLoopScheduler.class)
                        .componentClazz(GameLoopScheduler.class)
                        .build())
                .componentRegister(GameLoopComponentRegister.<GameLoopTickManager>builder()
                        .interfaceClazz(IGameLoopTickManager.class)
                        .componentClazz(GameLoopTickManager.class)
                        .build())
                .build();
    }

    @Bean(name = "defaultGameLoop")
    @ConditionalOnMissingBean(value = IGameLoop.class, name = "defaultGameLoop")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoop gameLoop(GameLoopConfig config) {
        final GameLoop gameLoop = new GameLoop(config);
        //注册自己，自我管理
        gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(gameLoop));
        //抛事件
        gameLoop.submit(IGameLoopEventBusFunction.post(new EventGameLoopCreatePost(gameLoop)));
        return gameLoop;
    }

    @Bean(name = "workers")
    @ConditionalOnMissingBean(value = IGameLoopGroup.class, name = "workers")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoopGroup gameLoopGroup(@Qualifier("defaultGameLoopConfig") GameLoopConfig config) {

        final int processors = Runtime.getRuntime().availableProcessors();
        final IGameLoop[] iGameLoops = IntStream.rangeClosed(1, processors)
                .mapToObj(i -> context.getBean(IGameLoop.class, GameLoopConfig.builder()
                        .id("worker-" + gameLoopCounter.getAndIncrement())
                        .componentRegisters(config.getComponentRegisters())
                        .build()))
                .toArray(IGameLoop[]::new);

        return new GameLoopGroup("workers", iGameLoops);
    }
}
