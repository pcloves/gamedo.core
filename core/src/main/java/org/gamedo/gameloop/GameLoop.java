package org.gamedo.gameloop;

import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.concurrent.NamedThreadFactory;
import org.gamedo.ecs.Entity;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.springframework.context.ApplicationContext;

import java.util.Map;
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

    public GameLoop(String id, final GameLoopConfig gameLoopConfig, ApplicationContext applicationContext) {
        this(id);

        gameLoopConfig.componentMap(this, applicationContext).forEach((k, v) -> componentMap.put(k, v));
    }

    public GameLoop(String id, final GameLoopConfig gameLoopConfig) {
        this(id);

        gameLoopConfig.componentMap(this).forEach((k, v) -> componentMap.put(k, v));
    }

    @Override
    public <T> boolean hasComponent(Class<T> interfaceClazz) {
        checkInThread();
        return super.hasComponent(interfaceClazz);
    }

    @Override
    public <T> Optional<T> getComponent(Class<T> interfaceClazz) {
        checkInThread();
        return super.getComponent(interfaceClazz);
    }

    @Override
    public Map<Class<?>, Object> getComponentMap() {
        checkInThread();
        return super.getComponentMap();
    }

    @Override
    public <T, R extends T> boolean addComponent(Class<T> interfaceClazz, R component) {
        checkInThread();
        return super.addComponent(interfaceClazz, component);
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

    private void checkInThread() {
        if (!inThread()) {
            throw new GameLoopException("call from anthor thread, gameLoop id:" + id +
                    ", called thread:" + Thread.currentThread().getName());
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
