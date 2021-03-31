package org.gamedo.gameloop.interfaces;

import java.util.List;
import java.util.function.Function;

/**
 * @param <T> 要选择的参数类型
 */
@FunctionalInterface
public interface GameLoopSelector<T> extends Function<T, List<IGameLoop>> {

    @Override
    List<IGameLoop> apply(T t);
}
