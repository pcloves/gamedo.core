package org.gamedo.gameloop;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.gamedo.annotation.GamedoComponent;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.utils.Pair;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Builder
@Value
public class GameLoopConfig {

    /**
     * gameLoop的id前缀
     */
    String gameLoopIdPrefix;
    /**
     * gameLoop的数量
     */
    int gameLoopCount;
    /**
     * 所属gameLoopGroup的id
     */
    String gameLoopGroupId;
    /**
     * gameLoop的组件列表
     */
    @Singular
    List<GameLoopComponentRegister<? extends GameLoopComponent>> componentRegisters;
    AtomicInteger gameLoopIdCounter = new AtomicInteger(1);

    public Map<Class<? super GameLoopComponent>, GameLoopComponent> componentMap(IGameLoop gameLoop,
                                                                                 ApplicationContext applicationContext) {

        return componentRegisters.stream()
                .flatMap(register -> {
                    final List<Class<?>> interfaceClazz = (List<Class<?>>) register.getInterfaceClazz();
                    final Class<? extends GameLoopComponent> componentClazz = register.getComponentClazz();
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
                        final List<Class<?>> interfaceClazz = (List<Class<?>>) register.getInterfaceClazz();
                        final Class<? extends GameLoopComponent> componentClazz = register.getComponentClazz();
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
