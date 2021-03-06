package org.gamedo.gameloop;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.*;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.GameLoopScheduler;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.GameLoopTickManager;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.util.Pair;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameLoopConfig {

    public static final GameLoopConfig DEFAULT = builder()
            .gameLoopGroupId("defaults")
            .nodeCountPerGameLoop(500)
            .gameLoopIdPrefix("default-")
            .gameLoopIdCounter(new AtomicInteger(1))
            .gameLoopCount(Runtime.getRuntime().availableProcessors() + 1)
            .daemon(false)
            .gameLoopImplClazz(GameLoop.class)
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


    /**
     * gameLoop???id??????
     */
    private String gameLoopIdPrefix = "default-";

    /**
     * gameLoopId????????????????????????gameLoopIdPrefix????????????gameLoop???id
     */
    private AtomicInteger gameLoopIdCounter = new AtomicInteger(1);

    /**
     * ?????????????????????
     */
    private boolean daemon;

    /**
     * {@link IGameLoop}????????????????????????????????????{@link GameLoop#GameLoop(GameLoopConfig)}???
     * {@link GameLoop#GameLoop(GameLoopConfig, MeterRegistry)}??????????????????
     */
    private Class<? extends IGameLoop> gameLoopImplClazz = GameLoop.class;

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
    private List<GameLoopComponentRegister> componentRegisters = new ArrayList<>(4);

    public Map<Class<?>, GameLoopComponent> componentMap(IGameLoop gameLoop) {

        return componentRegisters.stream()
                .flatMap(register -> {
                    try {
                        final List<Class<?>> interfaceClazz = register.getAllInterfaces();
                        final Class<? extends GameLoopComponent> componentClazz = register.getImplementation();
                        final Constructor<? extends GameLoopComponent> constructor = componentClazz.getConstructor(IGameLoop.class);
                        final GameLoopComponent gameLoopComponent = constructor.newInstance(gameLoop);

                        final List<Class<?>> noInterfaces = interfaceClazz.stream()
                                .filter(clazz -> !clazz.isInstance(gameLoopComponent))
                                .collect(Collectors.toList());

                        if (!noInterfaces.isEmpty()) {
                            throw new GameLoopException("illegal interfaces: " + noInterfaces + " for " + gameLoopComponent.getClass().getName());
                        }

                        return interfaceClazz.stream().map(k -> Pair.of(k, gameLoopComponent));
                    } catch (Throwable t) {
                        throw new GameLoopException("instantiate GameLoopComponentRegister failed, register:" + register, t);
                    }
                })
                .collect(Collectors.toMap(pair -> (Class<?>) pair.getK(), Pair::getV));
    }
}
