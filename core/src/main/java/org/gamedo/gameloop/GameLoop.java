package org.gamedo.gameloop;

import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.concurrent.NamedThreadFactory;
import org.gamedo.ecs.Entity;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
public class GameLoop extends Entity implements IGameLoop {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected final Optional<IGameLoop> gameLoopOptional = Optional.of(this);
    @Delegate(types = ScheduledExecutorService.class)
    protected final ScheduledExecutorService delegate;
    protected volatile Thread currentThread;

    public GameLoop(final String id) {

        super(id);

        delegate = new SingleThreadScheduledThreadPoolExecutor(id);
    }

    @Override
    public boolean inThread() {
        return currentThread == Thread.currentThread();
    }

    @Override
    public <R> CompletableFuture<R> submit(GameLoopFunction<R> function) {

        if (inThread()) {
            try {
                return CompletableFuture.completedFuture(function.apply(this));
            } catch (Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
        } else {
            return CompletableFuture.supplyAsync(() -> function.apply(this), this);
        }
    }

    private class SingleThreadScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
        private SingleThreadScheduledThreadPoolExecutor(String id) {
            super(1, new NamedThreadFactory(id));
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);

            //原子操作
            synchronized (GameLoop.this) {
                currentThread = Thread.currentThread();
                GameLoops.GAME_LOOP_THREAD_LOCAL.set(gameLoopOptional);
            }
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            //原子操作
            synchronized (GameLoop.this) {
                GameLoops.GAME_LOOP_THREAD_LOCAL.set(Optional.empty());
                currentThread = null;
            }
        }
    }
}
