package org.gamedo.gameloop;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.concurrent.NamedThreadFactory;
import org.gamedo.ecs.impl.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IGameLoopAction;
import org.gamedo.event.imp.EventBus;
import org.gamedo.event.interfaces.IEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GameLoop extends Entity implements IGameLoop
{
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<IGameLoop> gameLoopOptional = Optional.of(this);
    private final IEventBus eventBus;
    private final List<IEntity> entityList = new ArrayList<>();
    private final ScheduledExecutorService scheduledExecutorService;

    private ScheduledFuture<?> future;
    private long lastTickMilliSecond;
    private long lastTickInterval;
    private volatile Thread thread;

    public GameLoop(final String id) {

        super(id);

        scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(id)) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);

                thread = Thread.currentThread();
                GameLoops.GAME_LOOP_THREAD_LOCAL.set(gameLoopOptional);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);

                GameLoops.GAME_LOOP_THREAD_LOCAL.set(Optional.empty());
                thread = null;
            }
        };

        eventBus = new EventBus(this);
        addComponent(IEventBus.class, eventBus);
    }

    @Override
    public boolean inGameLoop() {
        return thread == Thread.currentThread();
    }

    @Override
    @Synchronized
    public boolean run(long initialDelay, long period, TimeUnit periodTimeUnit) {

        if (future != null) {
            return false;
        }

        lastTickMilliSecond = System.currentTimeMillis();

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final long currentTimeMillis = System.currentTimeMillis();

                    lastTickInterval = currentTimeMillis - lastTickMilliSecond;
                    lastTickMilliSecond = currentTimeMillis;

                    tick(lastTickInterval);

                    log.info("tick elapse:{}", System.currentTimeMillis() - currentTimeMillis);
                } catch (Throwable t) {
                    log.error("exception caught.", t);
                }
            }
        };

        future = scheduledExecutorService.scheduleAtFixedRate(runnable, initialDelay, period, periodTimeUnit);

        return true;
    }

    @Override
    public boolean shutdown(long timeout, TimeUnit timeUnit) throws InterruptedException {

        //TODO:这里还得好好想一下
        scheduledExecutorService.shutdown();

        return scheduledExecutorService.awaitTermination(timeout, timeUnit);
    }

    @Override
    public boolean registerEntity(IEntity entity) {

        if (inGameLoop()) {
            if (entityList.contains(entity)) {
                return false;
            }

            return entityList.add(entity);
        }
        else {
            log.error("should not register an entity in another thread, entity:" + entity, new Throwable());
            throw new RuntimeException("should not register an entity in another thread, entity:" + entity);
        }
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(IGameLoopAction<T> action) {

        if (inGameLoop()) {
            return CompletableFuture.completedFuture(action.apply(this));
        }
        else {
            return CompletableFuture.supplyAsync(() -> action.apply(this), this);
        }
    }

    @Override
    public void tick(long elapse) {
        entityList.forEach(entity -> safeTick(entity, elapse));
    }

    @Override
    public void execute(Runnable command) {
        scheduledExecutorService.execute(command);
    }

    private static void safeTick(final IEntity entity, long elapse) {
        try {
            entity.tick(elapse);
        } catch (Throwable e) {
            log.error("exception caught, entity:" + entity, e);
        }
    }
}
