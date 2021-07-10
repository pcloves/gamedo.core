package org.gamedo.gameloop.interfaces;

import java.util.function.Predicate;

@FunctionalInterface
public interface GameLoopPredicate extends Predicate<IGameLoop> {

    @Override
    boolean test(IGameLoop iGameLoop);
}
