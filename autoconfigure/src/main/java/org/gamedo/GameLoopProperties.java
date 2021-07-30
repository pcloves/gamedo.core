package org.gamedo;

import lombok.Data;
import org.gamedo.gameloop.GameLoopComponentRegister;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.GameLoopScheduler;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.GameLoopTickManager;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gamedo.gameloop")
@Data
public class GameLoopProperties {

    public static final GameLoopConfig DEFAULT = GameLoopConfig.builder()
            .gameLoopIdPrefix("default-")
            .gameLoopGroupId("defaultGroup")
            .gameLoopCount(Runtime.getRuntime().availableProcessors())
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

    /**
     * 默认配置
     */
    private GameLoopConfig gameLoopConfigDefault = DEFAULT;
    /**
     * cpu密集型线程池配置
     */
    private GameLoopConfig gameLoopConfigWorker = GameLoopConfig.builder()
            .gameLoopIdPrefix("worker-")
            .gameLoopGroupId("workerGroup")
            .gameLoopCount(Runtime.getRuntime().availableProcessors())
            .componentRegisters(DEFAULT.getComponentRegisters())
            .build();
    /**
     * io密集型线程池配置
     */
    private GameLoopConfig gameLoopConfigIo = GameLoopConfig.builder()
            .gameLoopIdPrefix("io-")
            .gameLoopGroupId("ioGroup")
            .gameLoopCount(Runtime.getRuntime().availableProcessors() * 10)
            .componentRegisters(DEFAULT.getComponentRegisters())
            .build();
    /**
     * 强一致性操作线程池配置
     */
    private GameLoopConfig gameLoopConfigSingle = GameLoopConfig.builder()
            .gameLoopIdPrefix("single-")
            .gameLoopGroupId("singleGroup")
            .gameLoopCount(1)
            .componentRegisters(DEFAULT.getComponentRegisters())
            .build();
}
