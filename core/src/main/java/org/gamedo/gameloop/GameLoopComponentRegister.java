package org.gamedo.gameloop;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.gamedo.ecs.GameLoopComponent;

import java.util.List;

@Value
@Builder
public class GameLoopComponentRegister<T extends GameLoopComponent> {
    @Singular("interfaceClazz")
    List<Class<? super T>> interfaceClazz;
    Class<T> componentClazz;
}
