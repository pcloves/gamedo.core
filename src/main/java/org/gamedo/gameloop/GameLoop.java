package org.gamedo.gameloop;

import lombok.Synchronized;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;
import org.gamedo.concurrent.NamedThreadFactory;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.components.EntityManager;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IEntityManager;
import org.gamedo.eventbus.EventBus;
import org.gamedo.eventbus.interfaces.IEventBus;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.scheduling.component.ScheduleRegister;
import org.gamedo.scheduling.interfaces.IScheduleRegister;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Log4j2
public class GameLoop extends Entity implements IGameLoop {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<IGameLoop> gameLoopOptional = Optional.of(this);
    private final IEntityManager entityMgr;

    @Delegate(types = ExecutorService.class)
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
                IGameLoop.GAME_LOOP_THREAD_LOCAL.set(gameLoopOptional);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);

                IGameLoop.GAME_LOOP_THREAD_LOCAL.set(Optional.empty());
                thread = null;
            }
        };

        //直接缓存起来，不用每次都查询组件了
        entityMgr = new EntityManager(this, this, null);

        addComponent(IEventBus.class, new EventBus(this));
        addComponent(IEntityManager.class, entityMgr);
        addComponent(IScheduleRegister.class, new ScheduleRegister(this, this));
    }

    public GameLoop(final Supplier<String> idSupplier) {
        this(idSupplier.get());
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

        final Runnable runnable = () -> {
                final long currentTimeMillis = System.currentTimeMillis();

                lastTickInterval = currentTimeMillis - lastTickMilliSecond;
                lastTickMilliSecond = currentTimeMillis;

                tick(lastTickInterval);
        };

        future = scheduledExecutorService.scheduleAtFixedRate(runnable, initialDelay, period, periodTimeUnit);
        return true;
    }

    @Override
    public <R> CompletableFuture<R> submit(GameLoopFunction<R> function) {

        if (inGameLoop()) {
            return CompletableFuture.completedFuture(function.apply(this));
        } else {
            return CompletableFuture.supplyAsync(() -> function.apply(this), this);
        }
    }

    @Override
    public void tick(long elapse) {
        //这里要是用副本，否则在tick期间可能会出现修改map的情况，当然有这里还有优化空间
        entityMgr.getEntityMap().forEach((entityId, entity) -> safeTick(entity, elapse));
    }

    private static void safeTick(final IEntity entity, long elapse) {
        try {
            entity.tick(elapse);
        } catch (Throwable e) {
            log.error("exception caught, entity:" + entity, e);
        }
    }
}
