package org.gamedo.gameloop;

import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.Tick;
import org.gamedo.configuration.GamedoConfiguration;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
@SpringBootTest(classes = GamedoConfiguration.class)
class GameLoopTest {
    private static final int DEFAULT_WAIT_TIMEOUT = 5;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;
    private static final String GAME_LOOP_ID = UUID.randomUUID().toString();
    private IGameLoop gameLoop;
    private final ConfigurableApplicationContext context;

    GameLoopTest(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    void setUp() {
        gameLoop = context.getBean(IGameLoop.class, GAME_LOOP_ID);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        gameLoop.shutdown();
        Assertions.assertDoesNotThrow(() -> gameLoop.awaitTermination(1, TimeUnit.MILLISECONDS));
    }

    @Test
    void testInGameLoop()
    {
        final CompletableFuture<Boolean> future1 = gameLoop.submit(iGameLoop -> iGameLoop.inGameLoop());
        final Boolean result1 = Assertions.assertDoesNotThrow(() -> future1.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertTrue(result1);

        final CompletableFuture<Boolean> future2 = CompletableFuture.supplyAsync(() -> gameLoop.inGameLoop());
        final Boolean result2 = Assertions.assertDoesNotThrow(() -> future2.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertFalse(result2);

        final CompletableFuture<Boolean> future3 = CompletableFuture.supplyAsync(() -> gameLoop.inGameLoop(), gameLoop);
        final Boolean result3 = Assertions.assertDoesNotThrow(() -> future3.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertTrue(result3);

        final CompletableFuture<Boolean> future4 = gameLoop.submit(iGameLoop -> IGameLoop.currentGameLoop()
                .map(iGameLoop1 -> iGameLoop1.inGameLoop())
                .orElse(false));
        final Boolean result4 = Assertions.assertDoesNotThrow(() -> future4.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertTrue(result4);

        final CompletableFuture<Boolean> future5 = CompletableFuture.supplyAsync(() -> IGameLoop.currentGameLoop()
                .map(iGameLoop -> iGameLoop.inGameLoop())
                .orElse(false));
        final Boolean result5 = Assertions.assertDoesNotThrow(() -> future5.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertFalse(result5);

        final CompletableFuture<Boolean> future6 = CompletableFuture.supplyAsync(() -> IGameLoop.currentGameLoop()
                .map(iGameLoop -> iGameLoop.inGameLoop())
                .orElse(false), gameLoop);
        final Boolean result6 = Assertions.assertDoesNotThrow(() -> future6.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertTrue(result6);

        final CompletableFuture<List<Boolean>> future7 = new CompletableFuture<>();
        final int listSize = 100;
        final List<Boolean> inGameLoopList = new ArrayList<>(listSize);
        final IEntity entity = new MyEntity(inGameLoopList, listSize, future7);

        gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity));

        final List<Boolean> list = Assertions.assertDoesNotThrow(() -> future7.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertEquals(listSize, list.size());
        Assertions.assertTrue(list.stream().allMatch(Boolean::booleanValue));
    }

    @Test
    void testSendEvent1() {

        final CompletableFuture<Boolean> futureDone = new CompletableFuture<>();
        final CompletableFuture<Optional<Boolean>> future = gameLoop.submit(iGameLoop -> iGameLoop.getComponent(IGameLoopEventBus.class)
                .map(iEventBus -> {
                    throw new RuntimeException("TestException");
                }));

        future.whenCompleteAsync((aBoolean, throwable) -> {
            try {
                Assertions.assertTrue(throwable instanceof RuntimeException);
                Assertions.assertTrue(gameLoop.inGameLoop());
            } catch (Throwable e) {
                futureDone.completeExceptionally(e);
            }

            futureDone.complete(true);
        }, gameLoop);

       Assertions.assertTrue(Assertions.assertDoesNotThrow(() -> futureDone.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT)));
    }

    @Test
    void testRegisterEntity()
    {
        final String entityId = "test1";
        final CompletableFuture<Boolean> future = gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(new Entity(entityId)));
        final Boolean registerResult = Assertions.assertDoesNotThrow(() -> future.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertTrue(registerResult);

        final CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> {
            return gameLoop.getComponent(IGameLoopEntityManager.class)
                    .map(iEntityMgr -> iEntityMgr.registerEntity(new Entity(entityId)))
                    .orElse(false);
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

    private static class MyEntity extends Entity {
        private final List<Boolean> inGameLoopList;
        private final int listSize;
        private final CompletableFuture<List<Boolean>> future7;

        private MyEntity(List<Boolean> inGameLoopList, int listSize, CompletableFuture<List<Boolean>> future7) {
            super("testEntity");
            this.inGameLoopList = inGameLoopList;
            this.listSize = listSize;
            this.future7 = future7;
        }

        @Tick
        public void myTick(Long currentMilliSecond, Long lastTickMilliSecond) {
            try {
                if (inGameLoopList.size() >= listSize) {
                    future7.complete(inGameLoopList);
                }
                else {
                    Optional<IGameLoop> iGameLoop = IGameLoop.currentGameLoop();
                    inGameLoopList.add(iGameLoop
                            .map(gameLoop -> gameLoop.inGameLoop())
                            .orElse(false));
                }
            } catch (Exception e) {
                future7.completeExceptionally(e);
            }
        }
    }
}