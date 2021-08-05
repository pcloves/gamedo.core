package org.gamedo.gameloop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Entity;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

@Log4j2
public class GameLoop extends Entity implements IGameLoop {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected final Optional<IGameLoop> gameLoopOptional = Optional.of(this);
    @Delegate(types = ScheduledExecutorService.class)
    protected final ScheduledExecutorService delegate;
    protected volatile Thread currentThread;
    private volatile IGameLoopGroup owner;

    public GameLoop(final String id) {
        super(id);

        delegate = new GameLoopScheduledExecutorService(this, id, false);
    }

    public GameLoop(final String id, boolean daemon) {
        super(id);

        delegate = new GameLoopScheduledExecutorService(this, id, daemon);
    }

    public GameLoop(final GameLoopConfig gameLoopConfig) {
        this(gameLoopConfig.getGameLoopIdPrefix() + gameLoopConfig.getGameLoopIdCounter().getAndIncrement(),
                gameLoopConfig.isDaemon());

        gameLoopConfig.componentMap(this).forEach((k, v) -> componentMap.put(k, v));
    }

    public GameLoop(final GameLoopConfig gameLoopConfig, MeterRegistry meterRegistry) {
        super(gameLoopConfig.getGameLoopIdPrefix() + gameLoopConfig.getGameLoopIdCounter().getAndIncrement());

        final boolean daemon = gameLoopConfig.isDaemon();
        final GameLoopScheduledExecutorService executorService = new GameLoopScheduledExecutorService(this, id, daemon);
        final Tags tags = Tags.of("owner", gameLoopConfig.getGameLoopGroupId());

        delegate = ExecutorServiceMetrics.monitor(meterRegistry, executorService, id, tags);

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
    public Optional<IGameLoopGroup> owner() {
        return Optional.ofNullable(owner);
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

    public void setOwner(IGameLoopGroup gameLoopGroup) {
        owner = gameLoopGroup;
    }

}
