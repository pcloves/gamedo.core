package org.gamedo.configuration;

import lombok.*;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopComponentRegister;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.GameLoopScheduler;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.GameLoopTickManager;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.util.GamedoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "gamedo.gameloop")
@Data
public class GameLoopProperties {

    public static final GameLoopConfigInner DEFAULT = GameLoopConfigInner.builder()
            .gameLoopGroupId("default")
            .nodeCountPerGameLoop(500)
            .gameLoopIdPrefix("default-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() + 1)
            .daemon(true)
            .gameLoopImplClazz(GameLoop.class.getName())
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
     * ?????????????????????
     */
    private GameLoopConfigInner defaults = GameLoopConfigInner.builder()
            .gameLoopGroupId("default")
            .nodeCountPerGameLoop(500)
            .gameLoopIdPrefix("default-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() + 1)
            .daemon(true)
            .gameLoopImplClazz(GameLoop.class.getName())
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * cpu????????????????????????
     */
    private GameLoopConfigInner worker = GameLoopConfigInner.builder()
            .gameLoopGroupId("worker")
            .nodeCountPerGameLoop(0)
            .gameLoopIdPrefix("worker-")
            .gameLoopIdCounter(1)
            .gameLoopCount(1)
            .daemon(false)
            .gameLoopImplClazz(GameLoop.class.getName())
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * ??????io????????????????????????
     */
    private GameLoopConfigInner io = GameLoopConfigInner.builder()
            .gameLoopGroupId("io")
            .nodeCountPerGameLoop(500)
            .gameLoopIdPrefix("io-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() * 10)
            .daemon(true)
            .gameLoopImplClazz(GameLoop.class.getName())
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * db???????????????
     */
    private GameLoopConfigInner db = GameLoopConfigInner.builder()
            .gameLoopGroupId("db")
            .nodeCountPerGameLoop(500)
            .gameLoopIdPrefix("db-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() * 10)
            .daemon(true)
            .gameLoopImplClazz(GameLoop.class.getName())
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * redis???????????????
     */
    private GameLoopConfigInner redis = GameLoopConfigInner.builder()
            .gameLoopGroupId("redis")
            .nodeCountPerGameLoop(500)
            .gameLoopIdPrefix("redis-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() * 10)
            .daemon(true)
            .gameLoopImplClazz(GameLoop.class.getName())
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * http?????????????????????
     */
    private GameLoopConfigInner http = GameLoopConfigInner.builder()
            .gameLoopGroupId("http")
            .nodeCountPerGameLoop(500)
            .gameLoopIdPrefix("http-")
            .gameLoopIdCounter(1)
            .gameLoopCount(Runtime.getRuntime().availableProcessors() * 10)
            .daemon(true)
            .gameLoopImplClazz(GameLoop.class.getName())
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * ?????????????????????????????????????????????
     */
    private GameLoopConfigInner single = GameLoopConfigInner.builder()
            .gameLoopGroupId("single")
            .nodeCountPerGameLoop(0)
            .gameLoopIdPrefix("single-")
            .gameLoopIdCounter(1)
            .gameLoopCount(1)
            .daemon(true)
            .gameLoopImplClazz(GameLoop.class.getName())
            .componentRegisters(DEFAULT.componentRegisters)
            .build();

    /**
     * {@link IGameLoopEventBus#post(IEvent)}?????????????????????????????????
     */
    private int maxEventPostDepth = GamedoConfiguration.MAX_EVENT_POST_DEPTH_DEFAULT;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameLoopConfigInner {
        /**
         * gameLoop???id??????
         */
        private String gameLoopIdPrefix = "default-";

        /**
         * gameLoopId?????????????????????????????????gameLoopIdPrefix????????????gameLoop???id
         */
        private int gameLoopIdCounter = 1;

        /**
         * ?????????????????????
         */
        private boolean daemon;

        /**
         * {@link IGameLoop}????????????
         */
        private String gameLoopImplClazz = GameLoop.class.getName();

        /**
         * gameLoop?????????
         */
        private int gameLoopCount = Runtime.getRuntime().availableProcessors();

        /**
         * ??????gameLoopGroup???id
         */
        private String gameLoopGroupId = "defaults";

        /**
         * ??????????????????hash????????????{@link IGameLoop}??????hash????????????{@link IGameLoop}????????????????????????
         */
        private int nodeCountPerGameLoop = 500;

        /**
         * gameLoop???????????????
         */
        @Singular
        private List<GameLoopComponentRegisterInner> componentRegisters = new ArrayList<>();

        @SneakyThrows
        @SuppressWarnings("unchecked")
        public GameLoopConfig convert() {
            return GameLoopConfig.builder()
                    .gameLoopIdPrefix(gameLoopIdPrefix)
                    .nodeCountPerGameLoop(nodeCountPerGameLoop)
                    .gameLoopIdCounter(new AtomicInteger(gameLoopIdCounter))
                    .daemon(daemon)
                    .gameLoopCount(gameLoopCount)
                    .gameLoopGroupId(gameLoopGroupId)
                    .gameLoopImplClazz((Class<? extends IGameLoop>) Class.forName(gameLoopImplClazz))
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
    public static class GameLoopComponentRegisterInner {
        /**
         * ?????????????????????????????????
         */
        @Singular
        private List<String> allInterfaces;

        /**
         * ???????????????????????????
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
