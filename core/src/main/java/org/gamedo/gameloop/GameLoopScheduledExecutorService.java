package org.gamedo.gameloop;

import org.gamedo.concurrent.NamedThreadFactory;

import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class GameLoopScheduledExecutorService extends ScheduledThreadPoolExecutor {
    private final GameLoop gameLoop;

    public GameLoopScheduledExecutorService(GameLoop gameLoop, String id, boolean daemon) {
        super(1, new NamedThreadFactory(id, daemon));
        this.gameLoop = gameLoop;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);

        //原子操作
        synchronized (gameLoop) {
            gameLoop.currentThread = Thread.currentThread();
            GameLoops.GAME_LOOP_THREAD_LOCAL.set(gameLoop.gameLoopOptional);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        //原子操作
        synchronized (gameLoop) {
            GameLoops.GAME_LOOP_THREAD_LOCAL.set(Optional.empty());
            gameLoop.currentThread = null;
        }
    }
}
