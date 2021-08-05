package org.gamedo.configuration;

import lombok.*;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.exception.GameLoopException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "gamedo.gameloop")
@Data
public class GameLoopProperties {

    public static final GameLoopConfigInner DEFAULT = GameLoopConfigInner.builder()
            .gameLoopIdPrefix("default-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() + 1)
            .gameLoopGroupId("defaults")
            .componentRegister(GameLoopComponentRegisterInner.builder()
                    .allInterface(IGameLoopEntityManager.class.getName())
                    .implementation(GameLoopEntityManager.class.getName())
                    .build())
            .componentRegister(GameLoopComponentRegisterInner.builder()
                    .allInterface(IGameLoopEventBus.class.getName())
                    .implementation(GameLoopEventBus.class.getName())
                    .build())
            .componentRegister(GameLoopComponentRegisterInner.builder()
                    .allInterface(IGameLoopScheduler.class.getName())
                    .implementation(GameLoopScheduler.class.getName())
                    .build())
            .componentRegister(GameLoopComponentRegisterInner.builder()
                    .allInterface(IGameLoopTickManager.class.getName())
                    .implementation(GameLoopTickManager.class.getName())
                    .build())
            .build();

    /**
     * 默认线程池配置
     */
    private GameLoopConfigInner defaults = GameLoopConfigInner.builder()
            .gameLoopIdPrefix("default-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() + 1)
            .gameLoopGroupId("defaults")
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * cpu密集型线程池配置
     */
    private GameLoopConfigInner workers =GameLoopConfigInner.builder()
            .gameLoopIdPrefix("worker-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() + 1)
            .gameLoopGroupId("workers")
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * io密集型线程池配置
     */
    private GameLoopConfigInner ios =GameLoopConfigInner.builder()
            .gameLoopIdPrefix("io-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() * 10)
            .gameLoopGroupId("ios")
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * 强一致性操作线程池配置（只有一个线程）
     */
    private GameLoopConfigInner singles = GameLoopConfigInner.builder()
            .gameLoopIdPrefix("single-")
            .gameLoopIdCounter(1)
            .gameLoopCount(1)
            .gameLoopGroupId("single")
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameLoopConfigInner
    {
        /**
         * gameLoop的id前缀
         */
        private String gameLoopIdPrefix = "default-";

        /**
         * gameLoopId的递增计数器起始值，和gameLoopIdPrefix共同生成gameLoop的id
         */
        private int gameLoopIdCounter = 1;

        /**
         * 是否为后台线程
         */
        private boolean daemon;

        /**
         * gameLoop的数量
         */
        private int gameLoopCount = Runtime.getRuntime().availableProcessors();

        /**
         * 所属gameLoopGroup的id
         */
        private String gameLoopGroupId = "defaults";

        /**
         * gameLoop的组件列表
         */
        @Singular
        private List<GameLoopComponentRegisterInner> componentRegisters = new ArrayList<>();

        public GameLoopConfig convert() {
            return GameLoopConfig.builder()
                    .gameLoopIdPrefix(gameLoopIdPrefix)
                    .gameLoopIdCounter(new AtomicInteger(gameLoopIdCounter))
                    .daemon(daemon)
                    .gameLoopCount(gameLoopCount)
                    .gameLoopGroupId(gameLoopGroupId)
                    .componentRegisters(componentRegisters.stream()
                            .map(GameLoopComponentRegisterInner::convert)
                            .collect(Collectors.toList())
                    )
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameLoopComponentRegisterInner
    {
        /**
         * 接口类的全限定类名列表
         */
        @Singular
        private List<String> allInterfaces;

        /**
         * 实现类的全限定类名
         */
        private String implementation;

        public GameLoopComponentRegister convert() {
            try {
                return GameLoopComponentRegister.builder()
                        .allInterfaces(allInterfaces.stream()
                                .map(GameLoopComponentRegisterInner::toInterface)
                                .collect(Collectors.toList()))
                        .implementation(toImplementation(implementation))
                        .build();
            } catch (Throwable e) {
                throw new GameLoopException("", e);
            }
        }

        private static Class<?> toInterface(String s) {
            try {
                return Class.forName(s);
            } catch (Throwable t) {
                throw new GameLoopException("Class.forName failed, clazz:" + s, t);
            }
        }

        @SuppressWarnings("unchecked")
        private static Class<GameLoopComponent> toImplementation(String s) {
            try {
                return (Class<GameLoopComponent>) Class.forName(s);
            } catch (Throwable t) {
                throw new GameLoopException("Class.forName failed, clazz:" + s, t);
            }
        }
    }
}
