package org.gamedo;

import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.components.eventbus.event.EventGameLoopCreatePost;
import org.gamedo.gameloop.functions.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.functions.IGameLoopEventBusFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

import java.util.Arrays;
import java.util.stream.IntStream;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GameLoopProperties.class)
@ComponentScan
public class GameLoopGroupAutoConfiguration {

    private final ApplicationContext context;
    private final GameLoopProperties properties;

    public GameLoopGroupAutoConfiguration(ApplicationContext context, GameLoopProperties properties) {
        this.context = context;
        this.properties = properties;
    }

    @Bean(name = "gameLoopConfig")
    @ConditionalOnMissingBean(value = GameLoopConfig.class, name = "gameLoopConfig")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    GameLoopConfig gameLoopConfig() {
        final GameLoopConfig defaults = properties.getDefaults().convert();

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
        return new GameLoop(config, context);
    }

    @Bean(name = "gameLoopGroup")
    @ConditionalOnMissingBean(value = IGameLoopGroup.class, name = "gameLoopGroup")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    IGameLoopGroup gameLoopGroup(GameLoopConfig config) {

        final IGameLoop[] iGameLoops = IntStream.rangeClosed(1, config.getGameLoopCount())
                .mapToObj(i -> context.getBean(IGameLoop.class, config))
                .peek(gameLoop -> gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(gameLoop)))
                .toArray(IGameLoop[]::new);

        final IGameLoopGroup gameLoopGroup = new GameLoopGroup(config.getGameLoopGroupId(), iGameLoops);

        Arrays.stream(gameLoopGroup.selectAll())
                .peek(gameLoop -> ((GameLoop) gameLoop).setOwner(gameLoopGroup))
                .map(gameLoop -> IGameLoopEventBusFunction.post(new EventGameLoopCreatePost(gameLoop.getId())))
                .forEach(post -> gameLoopGroup.submitAll(post));

        return gameLoopGroup;
    }
}
