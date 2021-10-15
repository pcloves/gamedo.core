package org.gamedo.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.gamedo.Gamedo;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.components.eventbus.event.EventGameLoopCreatePost;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.util.GamedoConfiguration;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GamedoProperties.class, GameLoopProperties.class, MetricProperties.class})
public class GameLoopGroupAutoConfiguration {

    private final ApplicationContext context;
    private final GamedoProperties gamedoProperties;
    private final GameLoopProperties gameLoopProperties;
    private final MetricProperties metricProperties;

    public GameLoopGroupAutoConfiguration(ApplicationContext context,
                                          GamedoProperties gamedoProperties,
                                          GameLoopProperties gameLoopProperties,
                                          MetricProperties metricProperties) {
        this.context = context;
        this.gamedoProperties = gamedoProperties;
        this.gameLoopProperties = gameLoopProperties;
        this.metricProperties = metricProperties;

        updateSystemProperty();
    }

    private void updateSystemProperty() {
        System.setProperty(GamedoConfiguration.MAX_EVENT_POST_DEPTH_KEY,
                String.valueOf(gameLoopProperties.getMaxEventPostDepth()));
        System.setProperty(GamedoConfiguration.GAMEDO_METRIC_ENTITY_ENABLE_KEY,
                String.valueOf(metricProperties.isEnable() && metricProperties.isEntityEnable()));
        System.setProperty(GamedoConfiguration.GAMEDO_METRIC_EVENT_ENABLE_KEY,
                String.valueOf(metricProperties.isEnable() && metricProperties.isEventEnable()));
        System.setProperty(GamedoConfiguration.GAMEDO_METRIC_CRON_ENABLE_KEY,
                String.valueOf(metricProperties.isEnable() && metricProperties.isCronEnable()));
        System.setProperty(GamedoConfiguration.GAMEDO_METRIC_TICK_ENABLE_KEY,
                String.valueOf(metricProperties.isEnable() && metricProperties.isTickEnable()));
    }

    @Bean
    @ConditionalOnMissingBean(Gamedo.class)
    Gamedo gamedo(ApplicationContext applicationContext) {
        return new Gamedo(applicationContext) {
        };
    }

    @Bean(name = "gameLoopConfig")
    @ConditionalOnMissingBean(value = GameLoopConfig.class, name = "gameLoopConfig")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    GameLoopConfig gameLoopConfig() {
        final GameLoopConfig defaults = gameLoopProperties.getDefaults().convert();

        return GameLoopConfig.builder()
                .gameLoopGroupId(defaults.getGameLoopGroupId())
                .nodeCountPerGameLoop(defaults.getNodeCountPerGameLoop())
                .gameLoopIdCounter(defaults.getGameLoopIdCounter())
                .gameLoopIdPrefix(defaults.getGameLoopIdPrefix())
                .daemon(defaults.isDaemon())
                .gameLoopCount(defaults.getGameLoopCount())
                .gameLoopIdCounter(defaults.getGameLoopIdCounter())
                .gameLoopImplClazz(GameLoop.class)
                .componentRegisters(defaults.getComponentRegisters())
                .build();
    }

    @Bean(name = "gameLoop")
    @ConditionalOnMissingBean(value = IGameLoop.class, name = "gameLoop")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoop gameLoop(GameLoopConfig config) throws NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {

        final boolean metricEnable = metricProperties.isEnable() &&
                !metricProperties.getDisabledGameLoopGroup().contains(config.getGameLoopGroupId()) &&
                context.getBeanNamesForType(MeterRegistry.class).length > 0;
        final Class<? extends IGameLoop> gameLoopClazz = config.getGameLoopImplClazz();
        return metricEnable ? gameLoopClazz.getConstructor(GameLoopConfig.class, MeterRegistry.class)
                .newInstance(config, context.getBean(MeterRegistry.class)) :
                gameLoopClazz.getConstructor(GameLoopConfig.class).newInstance(config);
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
