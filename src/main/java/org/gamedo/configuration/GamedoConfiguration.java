package org.gamedo.configuration;

import lombok.extern.log4j.Log4j2;
import org.gamedo.application.ApplicationComponentRegister;
import org.gamedo.application.GamedoApplication;
import org.gamedo.ecs.components.GameLoopEntityManager;
import org.gamedo.ecs.interfaces.IApplication;
import org.gamedo.ecs.interfaces.IGameLoopEntityManager;
import org.gamedo.eventbus.GameLoopEventBus;
import org.gamedo.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopComponentRegister;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.scheduling.GameLoopScheduler;
import org.gamedo.scheduling.interfaces.IGameLoopScheduler;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

@SuppressWarnings("SpringFacetCodeInspection")
@Log4j2
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GamedoProperties.class)
public class GamedoConfiguration {

    private final AtomicInteger index = new AtomicInteger(1);
    private final AbstractApplicationContext applicationContext;

    public GamedoConfiguration(AbstractApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public IApplication gamedoApplication(GamedoProperties properties) {
        return new GamedoApplication(properties.getApplicationId(), applicationContext);
    }

    @Bean
    public ApplicationComponentRegister<GameLoopGroup> gameLoopGroupRegister() {
        final List<Class<? super GameLoopGroup>> classList = List.of(IGameLoopGroup.class);
        return new ApplicationComponentRegister<>(classList, applicationContext.getBean(GameLoopGroup.class));
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean
    public GameLoopGroup gameLoopGroup() {
        final String gameLoopGroupId = "GameLoopGroup" + index.getAndIncrement();
        final IGameLoop[] iGameLoops = applicationContext.getBean(IGameLoop[].class,
                gameLoopGroupId,
                Runtime.getRuntime().availableProcessors());

        return new GameLoopGroup(gameLoopGroupId, iGameLoops);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean
    public GameLoopGroup gameLoopGroup(String id) {
        final IGameLoop[] iGameLoops = applicationContext.getBean(IGameLoop[].class,
                id,
                Runtime.getRuntime().availableProcessors());

        return new GameLoopGroup(id, iGameLoops);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean
    public GameLoopGroup gameLoopGroup(String id, int gameLoopCount) {
        final IGameLoop[] iGameLoops = applicationContext.getBean(IGameLoop[].class,
                id,
                gameLoopCount);

        return new GameLoopGroup(id, iGameLoops);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean(IGameLoop[].class)
    public IGameLoop[] gameLoops(String idPrefix, int size) {

        return IntStream.rangeClosed(1, size)
                .mapToObj(value -> applicationContext.getBean(IGameLoop.class, idPrefix + '-' + value))
                .toArray(IGameLoop[]::new);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean
    public GameLoop gameLoop(String id) {

        log.info("IGameLoop bean, id:{}", id);
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
                log.info("addComponent {}", () -> clazz.getName());
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

        final List<Class<? super GameLoopScheduler>> list = List.of(IGameLoopScheduler.class, GameLoopScheduler.class);
        return new GameLoopComponentRegister<>(list, new GameLoopScheduler(gameLoop));
    }
}
