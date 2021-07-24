package org.gamedo.gameloop;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.utils.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Builder
@Value
public class GameLoopConfig {
    String id;
    @Singular List<GameLoopComponentRegister<? extends GameLoopComponent>> componentRegisters;

    public Map<Class<? super GameLoopComponent>, GameLoopComponent> componentMap(IGameLoop gameLoop) {

        return componentRegisters.stream()
                .flatMap(register -> {

                    final List<Class<?>> interfaceClazz;
                    final Class<? extends GameLoopComponent> componentClazz = register.getComponentClazz();
                    try {
                        interfaceClazz = (List<Class<?>>) register.getInterfaceClazz();
                        final Constructor<? extends GameLoopComponent> constructor = componentClazz.getConstructor(IGameLoop.class);
                        final GameLoopComponent component = constructor.newInstance(gameLoop);
                        return interfaceClazz.stream().map(k -> Pair.of(k, component));
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new GameLoopException("instantiate '" + componentClazz.getSimpleName() +
                                "' failed, Is there a public consturctor with param:" +
                                IGameLoop.class.getSimpleName() + '?', e);
                    }

                })
                .collect(Collectors.toMap(pair -> (Class<? super GameLoopComponent>) pair.getK(), pair -> pair.getV()));
    }
}
