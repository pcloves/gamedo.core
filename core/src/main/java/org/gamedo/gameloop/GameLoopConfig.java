package org.gamedo.gameloop;

import lombok.*;
import org.gamedo.annotation.GamedoComponent;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.utils.Pair;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameLoopConfig {

    /**
     * gameLoop的id前缀
     */
    private String gameLoopIdPrefix = "default-";

    /**
     * gameLoopId的递增计数器，和gameLoopIdPrefix共同生成gameLoop的id
     */
    private AtomicInteger gameLoopIdCounter = new AtomicInteger(1);

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
    private List<GameLoopComponentRegister<? extends GameLoopComponent>> componentRegisters = new ArrayList<>(4);

    public Map<Class<? super GameLoopComponent>, GameLoopComponent> componentMap(IGameLoop gameLoop,
                                                                                 ApplicationContext applicationContext) {

        return componentRegisters.stream()
                .flatMap(register -> {
                    final List<Class<?>> interfaceClazz = (List<Class<?>>) register.getAllInterfaces();
                    final Class<? extends GameLoopComponent> componentClazz = register.getImplementation();
                    final GameLoopComponent gameLoopComponent;
                    try {
                        gameLoopComponent = applicationContext.getBean(componentClazz, gameLoop);
                    } catch (BeansException e) {
                        throw new GameLoopException("getBean failed for:" + componentClazz.getName() +
                                ", is it annotated by" + GamedoComponent.class.getName() + '?', e);
                    }
                    return interfaceClazz.stream().map(k -> Pair.of(k, gameLoopComponent));
                })
                .collect(Collectors.toMap(pair -> (Class<? super GameLoopComponent>) pair.getK(), pair -> pair.getV()));
    }

    public Map<Class<? super GameLoopComponent>, GameLoopComponent> componentMap(IGameLoop gameLoop) {

        return componentRegisters.stream()
                .flatMap(register -> {
                    try {
                        final List<Class<?>> interfaceClazz = (List<Class<?>>) register.getAllInterfaces();
                        final Class<? extends GameLoopComponent> componentClazz = register.getImplementation();
                        final Constructor<? extends GameLoopComponent> constructor = componentClazz.getConstructor(IGameLoop.class);
                        final GameLoopComponent gameLoopComponent = constructor.newInstance(gameLoop);

                        return interfaceClazz.stream().map(k -> Pair.of(k, gameLoopComponent));
                    } catch (Throwable t) {
                        throw new GameLoopException("instantiate GameLoopComponentRegister failed, register:" + register, t);
                    }
                })
                .collect(Collectors.toMap(pair -> (Class<? super GameLoopComponent>) pair.getK(), pair -> pair.getV()));
    }
}
