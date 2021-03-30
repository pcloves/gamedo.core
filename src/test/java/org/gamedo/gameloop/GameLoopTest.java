package org.gamedo.gameloop;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.impl.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.event.interfaces.EventPriority;
import org.gamedo.event.interfaces.IEvent;
import org.gamedo.event.interfaces.IEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
class GameLoopTest {
    private GameLoop gameLoop;

    @BeforeEach
    void setUp() {
        gameLoop = new GameLoop("testGameLoop");
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        gameLoop.shutdown(1, TimeUnit.SECONDS);
    }

    @Test
    void testInGameLoop()
    {
        final CompletableFuture<Boolean> future1 = gameLoop.executeAsync(gameLoop1 -> gameLoop1.inGameLoop());
        final Boolean executeAsyncResult1 = Assertions.assertDoesNotThrow(() -> future1.get());
        Assertions.assertTrue(executeAsyncResult1);

        final CompletableFuture<Boolean> future2 = CompletableFuture.supplyAsync(() -> gameLoop.inGameLoop());
        final Boolean executeAsyncResult2 = Assertions.assertDoesNotThrow(() -> future2.get());
        Assertions.assertFalse(executeAsyncResult2);

        final CompletableFuture<Boolean> future3 = new CompletableFuture<>();
        final List<Boolean> inGameLoopList = new CopyOnWriteArrayList<>();
        final IEntity entity = new Entity("testEntity") {
            @Override
            public void tick(long elapse)
            {
                log.info("tick, thread:{}, elapse:{}, time:{}", Thread.currentThread().getName(), elapse, System.currentTimeMillis());

                if (inGameLoopList.size() >= 10) {
                    future3.complete(true);
                }
                else {
                    inGameLoopList.add(GameLoops.currentGameLoop()
                                                .filter(Objects::nonNull)
                                                .map(gameLoop1 -> gameLoop1.inGameLoop())
                                                .orElse(false));
                }
            }
        };

        gameLoop.executeAsync(GameLoops.registerEntity(entity));
        gameLoop.run(50, 50, TimeUnit.MILLISECONDS);

        future3.join();

        Assertions.assertTrue(inGameLoopList.stream().allMatch(Boolean::booleanValue));
    }

    @Test
    void testCompleteableFuture() {
        gameLoop.run(10, 10, TimeUnit.MILLISECONDS);

        final CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> {

            log.info("thread:{}", Thread.currentThread().getName());
            final CompletableFuture<Boolean> future = gameLoop.executeAsync(iGameLoop -> {
                log.info("thread:{}", Thread.currentThread().getName());
                return iGameLoop.inGameLoop();
            });

            return Assertions.assertDoesNotThrow(() -> future.get());
        });

        future1.join();

        final Boolean inGameLoop1 = Assertions.assertDoesNotThrow(() -> future1.get());
        Assertions.assertTrue(inGameLoop1);

        final CompletableFuture<Boolean> future2 = gameLoop.executeAsync(gameLoop1 -> gameLoop1.inGameLoop());
        future2.join();

        final Boolean inGameLoop2 = Assertions.assertDoesNotThrow(() -> future2.get());
        Assertions.assertTrue(inGameLoop2);

        log.info("-----------------------------------------------------");
        final CompletableFuture<Boolean> future3 = CompletableFuture.supplyAsync(() -> {

            log.info("thread:{}", Thread.currentThread().getName());
            final CompletableFuture<Boolean> future = gameLoop.executeAsync(iGameLoop -> {
                log.info("thread:{}", Thread.currentThread().getName());
                return iGameLoop.inGameLoop();
            });

            return Assertions.assertDoesNotThrow(() -> future.get());
        }, gameLoop);

        future3.join();

        final Boolean inGameLoop3 = Assertions.assertDoesNotThrow(() -> future1.get());
        Assertions.assertTrue(inGameLoop3);
    }

    @Test
    void testCurrentGameLoop() {
        CompletableFuture<Optional<IGameLoop>> future = new CompletableFuture<>();
        final IEntity entity = new Entity("testEntity") {
            @Override
            public void tick(long elapse) {
                if (!future.isDone()) {
                    future.complete(GameLoops.currentGameLoop());
                }
            }
        };

        gameLoop.run(10, 10, TimeUnit.MICROSECONDS);
        gameLoop.executeAsync(GameLoops.registerEntity(entity));

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        final IGameLoop gameLoopExpected = Assertions.assertDoesNotThrow(() -> future.get().get());
        Assertions.assertEquals(gameLoop, gameLoopExpected);
    }

    @Test
    void testSendEvent() {

        @Value
        class MyEvent implements IEvent
        {
            int value;
        }

        final CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        gameLoop.executeAsync(gameLoop1 -> gameLoop1.getComponent(IEventBus.class)
                                                    .filter(Objects::nonNull)
                                                    .map(iEventBus -> {
                                                        return iEventBus.registerEvent(
                                                                MyEvent.class,
                                                                myEvent -> {completableFuture.complete(myEvent.value); return false;},
                                                                EventPriority.Normal);
                                                    }));

        final CompletableFuture<Optional<Boolean>> sendEventFuture = gameLoop.executeAsync(GameLoops.sendEvent(new MyEvent(3)));
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        final Boolean actualReturn = Assertions.assertDoesNotThrow(() -> sendEventFuture.get().get());
        Assertions.assertFalse(actualReturn);

        final Integer actual = Assertions.assertDoesNotThrow(() -> completableFuture.get());
        Assertions.assertEquals(3, actual);
    }

    @Test
    void testRegisterEntity()
    {
        final CompletableFuture<Boolean> future = gameLoop.executeAsync(GameLoops.registerEntity(new Entity("test1")));
        final Boolean registerResult = Assertions.assertDoesNotThrow(() -> future.get());
        Assertions.assertTrue(registerResult);

        final CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> {
            return gameLoop.registerEntity(new Entity("test"));
        });

        future1.whenComplete((r, t) -> {
            Assertions.assertNotNull(t);
            Assertions.assertTrue(t.getCause() instanceof RuntimeException);
        });
    }

    @Test
    void inGameLoop() {
    }

    @Test
    void postEvent() {
    }

}