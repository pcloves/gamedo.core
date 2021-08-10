package org.gamedo.gameloop.interfaces;

import lombok.extern.log4j.Log4j2;
import org.gamedo.GameLoopGroupConfiguration;
import org.gamedo.annotation.Tick;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.GameLoops;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
@SpringBootTest(classes = GameLoopGroupConfiguration.class)
class IGameLoopTest {
    private static final int DEFAULT_WAIT_TIMEOUT = 5;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;
    private IGameLoop gameLoop;
    private final ConfigurableApplicationContext context;

    IGameLoopTest(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    void setUp() {
        gameLoop = context.getBean(IGameLoop.class);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        gameLoop.shutdown();
        Assertions.assertDoesNotThrow(() -> gameLoop.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    void testProtoTypeGameLoopComponent() {
        final IGameLoop gameLoop1 = context.getBean(IGameLoop.class);
        final IGameLoop gameLoop2 = context.getBean(IGameLoop.class);

        final AtomicInteger integer = new AtomicInteger(1);
        final CompletableFuture<Integer> future1 = gameLoop1.submit(iGameLoop -> iGameLoop.getComponent(IGameLoopEntityManager.class)
                .map(iGameLoopEntityManager -> iGameLoopEntityManager.hashCode())
                .orElse(integer.getAndIncrement()));

        final CompletableFuture<Integer> future2 = gameLoop2.submit(iGameLoop -> iGameLoop.getComponent(IGameLoopEntityManager.class)
                .map(iGameLoopEntityManager -> iGameLoopEntityManager.hashCode())
                .orElse(integer.getAndIncrement()));

        final Integer hashCode1 = Assertions.assertDoesNotThrow(() -> future1.get());
        final Integer hashCode2 = Assertions.assertDoesNotThrow(() -> future2.get());

        Assertions.assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    void testInThread()
    {
        final CompletableFuture<Boolean> future1 = gameLoop.submit(iGameLoop -> iGameLoop.inThread());
        final Boolean result1 = Assertions.assertDoesNotThrow(() -> future1.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertTrue(result1);

        final CompletableFuture<Boolean> future2 = CompletableFuture.supplyAsync(() -> gameLoop.inThread());
        final Boolean result2 = Assertions.assertDoesNotThrow(() -> future2.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertFalse(result2);

        final CompletableFuture<Boolean> future3 = CompletableFuture.supplyAsync(() -> gameLoop.inThread(), gameLoop);
        final Boolean result3 = Assertions.assertDoesNotThrow(() -> future3.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertTrue(result3);

        final CompletableFuture<Boolean> future4 = gameLoop.submit(iGameLoop -> GameLoops.current()
                .map(iGameLoop1 -> iGameLoop1.inThread())
                .orElse(false));
        final Boolean result4 = Assertions.assertDoesNotThrow(() -> future4.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertTrue(result4);

        final CompletableFuture<Boolean> future5 = CompletableFuture.supplyAsync(() -> GameLoops.current()
                .map(iGameLoop -> iGameLoop.inThread())
                .orElse(false));
        final Boolean result5 = Assertions.assertDoesNotThrow(() -> future5.get(DEFAULT_WAIT_TIMEOUT, DEFAULT_TIME_UNIT));
        Assertions.assertFalse(result5);

        final CompletableFuture<Boolean> future6 = CompletableFuture.supplyAsync(() -> GameLoops.current()
                .map(iGameLoop -> iGameLoop.inThread())
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
    void testExceptionInCurrentThread() {

        final CompletableFuture<Boolean> futureDone = new CompletableFuture<>();
        final CompletableFuture<Optional<Boolean>> future = gameLoop.submit(iGameLoop -> iGameLoop.getComponent(IGameLoopEventBus.class)
                .map(iEventBus -> {
                    throw new RuntimeException("TestException");
                }));

        future.whenCompleteAsync((aBoolean, throwable) -> {
            try {
                Assertions.assertTrue(throwable instanceof RuntimeException);
                Assertions.assertTrue(gameLoop.inThread());
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
            Assertions.assertTrue(t.getCause() instanceof GameLoopException);
        });
    }

    @Test
    void testRegisterConcurrently() {
        final int entityCount = 1000;
        final Set<Entity> entitySet = IntStream.rangeClosed(1, entityCount)
                .boxed()
                .map(i -> new Entity("testRegisterConcurrently" + i))
                .collect(Collectors.toSet());

        for (Entity entity : entitySet) {
            final int availableProcessors = Runtime.getRuntime().availableProcessors();
            final List<Boolean> resultList = IntStream.rangeClosed(1, availableProcessors)
                    .boxed()
                    .parallel()
                    .map(i -> gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity)).join())
                    .collect(Collectors.toList());

            final long successCount = resultList.stream().filter(Boolean::booleanValue).count();
            final long failedCount = resultList.stream().filter(b -> !b).count();
            Assertions.assertEquals(1, successCount);
            Assertions.assertEquals(availableProcessors - 1, failedCount);
        }

        final Integer entityCountActual = gameLoop.submit(IGameLoopEntityManagerFunction.getEntityCount()).join();
        final Boolean selfRegister = gameLoop.submit(IGameLoopEntityManagerFunction.hasEntity(gameLoop.getId())).join();
        Assertions.assertEquals(entityCount + (selfRegister ? 1 : 0), entityCountActual);
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
        private final CompletableFuture<List<Boolean>> future;

        private MyEntity(List<Boolean> inGameLoopList, int listSize, CompletableFuture<List<Boolean>> future) {
            super("testEntity");
            this.inGameLoopList = inGameLoopList;
            this.listSize = listSize;
            this.future = future;
        }

        @Tick
        public void myTick(Long currentMilliSecond, Long lastTickMilliSecond) {
            try {
                if (!future.isDone()) {
                    if (inGameLoopList.size() >= listSize) {
                        future.complete(inGameLoopList);
                    }
                    else {
                        Optional<IGameLoop> iGameLoop = GameLoops.current();
                        inGameLoopList.add(iGameLoop
                                .map(gameLoop -> gameLoop.inThread())
                                .orElse(false));
                    }
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }
    }
}