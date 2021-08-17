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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;
import java.util.stream.IntStream;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GamedoProperties.class, GameLoopProperties.class})
@ComponentScan(basePackageClasses = Gamedo.class)
public class GameLoopGroupAutoConfiguration {

    private final ApplicationContext context;
    private final GameLoopProperties gameLoopProperties;
    private final MetricProperties metricProperties;

    public GameLoopGroupAutoConfiguration(ApplicationContext context,
                                          GameLoopProperties gameLoopProperties,
                                          MetricProperties metricProperties) {
        this.context = context;
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

    @Bean(name = "gameLoopConfig")
    @ConditionalOnMissingBean(value = GameLoopConfig.class, name = "gameLoopConfig")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    GameLoopConfig gameLoopConfig() {
        final GameLoopConfig defaults = gameLoopProperties.getDefaults().convert();

        return GameLoopConfig.builder()
                .gameLoopIdCounter(defaults.getGameLoopIdCounter())
                .gameLoopIdPrefix(defaults.getGameLoopIdPrefix())
                .daemon(defaults.isDaemon())
                .gameLoopGroupId(defaults.getGameLoopGroupId())
                .gameLoopCount(defaults.getGameLoopCount())
                .gameLoopIdCounter(defaults.getGameLoopIdCounter())
                .componentRegisters(defaults.getComponentRegisters())
                .build();
    }

    @Bean(name = "gameLoop")
    @ConditionalOnMissingBean(value = IGameLoop.class, name = "gameLoop")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoop gameLoop(GameLoopConfig config) {

        final boolean metricEnable = metricProperties.isEnable() &&
                !metricProperties.getDisabledGameLoopGroup().contains(config.getGameLoopGroupId());
        final MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);

        return metricEnable ? new GameLoop(config, meterRegistry) : new GameLoop(config);
    }

    @Bean(name = "gameLoopGroup")
    @ConditionalOnMissingBean(value = IGameLoopGroup.class, name = "gameLoopGroup")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoopGroup gameLoopGroup(GameLoopConfig config) {

        final IGameLoop[] iGameLoops = IntStream.rangeClosed(1, config.getGameLoopCount())
                .mapToObj(i -> context.getBean(IGameLoop.class, config))
                .toArray(IGameLoop[]::new);

        final IGameLoopGroup gameLoopGroup = new GameLoopGroup(config.getGameLoopGroupId(), iGameLoops);

        Arrays.stream(gameLoopGroup.selectAll())
                .peek(gameLoop -> ((GameLoop) gameLoop).setOwner(gameLoopGroup))
                .peek(gameLoop -> gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(gameLoop)))
                .map(gameLoop -> IGameLoopEventBusFunction.post(new EventGameLoopCreatePost(gameLoop.getId())))
                .forEach(post -> gameLoopGroup.submitAll(post));

        return gameLoopGroup;
    }
}
