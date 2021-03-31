package org.gamedo.gameloop.interfaces;

import org.gamedo.gameloop.GameLoop;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface IGameLoopGroup extends ExecutorService
{
    GameLoop next();

    <T> List<IGameLoop> select(GameLoopSelector<T> selector);
}
