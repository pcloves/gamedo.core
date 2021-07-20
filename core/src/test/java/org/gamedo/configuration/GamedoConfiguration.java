package org.gamedo.configuration;

import lombok.extern.slf4j.Slf4j;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopComponentRegister;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.GameLoopScheduler;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.GameLoopTickManager;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.ResolvableType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
@Configuration(proxyBeanMethods = false)
public class GamedoConfiguration {

    private final AtomicInteger index = new AtomicInteger(1);
    private final AbstractApplicationContext applicationContext;

    public GamedoConfiguration(AbstractApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GameLoopGroup gameLoopGroup() {
        final String gameLoopGroupId = "GameLoopGroup" + index.getAndIncrement();
        final IGameLoop[] iGameLoops = applicationContext.getBean(IGameLoop[].class,
                gameLoopGroupId,
                Runtime.getRuntime().availableProcessors());

        return new GameLoopGroup(gameLoopGroupId, iGameLoops);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GameLoopGroup gameLoopGroup(String id) {
        final IGameLoop[] iGameLoops = applicationContext.getBean(IGameLoop[].class,
                id,
                Runtime.getRuntime().availableProcessors());

        return new GameLoopGroup(id, iGameLoops);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GameLoopGroup gameLoopGroup(String id, int gameLoopCount) {
        final IGameLoop[] iGameLoops = applicationContext.getBean(IGameLoop[].class,
                id,
                gameLoopCount);

        return new GameLoopGroup(id, iGameLoops);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public IGameLoop[] gameLoops(String idPrefix, int size) {

        return IntStream.rangeClosed(1, size)
                .mapToObj(value -> applicationContext.getBean(IGameLoop.class, idPrefix + '-' + value))
                .toArray(IGameLoop[]::new);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GameLoop gameLoop(String id) {

        log.info("GameLoop bean, id:{}", id);
        final GameLoop gameLoop = new GameLoop(id);

        final ResolvableType resolvableType = ResolvableType.forRawClass(GameLoopComponentRegister.class);
        final String[] beanNamesForType = applicationContext.getBeanNamesForType(resolvableType);

        final List<GameLoopComponentRegister> collect = Arrays.stream(beanNamesForType)
                .map(s -> (GameLoopComponentRegister) applicationContext.getBean(s, gameLoop))
                .collect(Collectors.toList());

        for (GameLoopComponentRegister register : collect) {

            final List<Class> clazzList = register.getClazzList();
            final Object object = register.getObject();

            for (Class clazz : clazzList) {
                gameLoop.addComponent(clazz, object);
                log.info("addComponent {}", clazz.getSimpleName());
            }
        }

        return gameLoop;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GameLoopComponentRegister<GameLoopEventBus> gameLoopEventBusRegister(IGameLoop gameLoop) {
        return new GameLoopComponentRegister<>(List.of(IGameLoopEventBus.class), new GameLoopEventBus(gameLoop));
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GameLoopComponentRegister<GameLoopEntityManager> gameLoopEntityManagerRegister(IGameLoop gameLoop) {
        return new GameLoopComponentRegister<>(List.of(IGameLoopEntityManager.class), new GameLoopEntityManager(gameLoop));
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GameLoopComponentRegister<GameLoopScheduler> gameLoopSchedulerRegister(IGameLoop gameLoop) {

        final List<Class<? super GameLoopScheduler>> list = List.of(IGameLoopScheduler.class);
        return new GameLoopComponentRegister<>(list, new GameLoopScheduler(gameLoop));
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GameLoopComponentRegister<GameLoopTickManager> gameLoopTickManagerRegister(IGameLoop gameLoop) {
        return new GameLoopComponentRegister<>(List.of(IGameLoopTickManager.class), new GameLoopTickManager(gameLoop));
    }
}
