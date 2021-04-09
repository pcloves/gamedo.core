package org.gamedo.gameloop;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntityFunction;
import org.gamedo.ecs.interfaces.IEntityManagerFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Log4j2
@SpringBootTest
@SpringBootApplication
@EnableScheduling
class GameLoopGroupTest {

    private IGameLoopGroup gameLoopGroup;
    @Autowired
    private TaskScheduler taskScheduler;

    @BeforeEach
    void setUp() {
        gameLoopGroup = new GameLoopGroup("GameLoopGroup");
        final int gameLoopCount = gameLoopGroup.selectAll().length;
        IntStream.rangeClosed(1, gameLoopCount)
                .mapToObj(value -> gameLoopGroup.selectNext())
                .forEach(iGameLoop -> iGameLoop.submit(IEntityFunction.addComponent(TaskScheduler.class, taskScheduler)));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        gameLoopGroup.shutdown();
        final boolean b = gameLoopGroup.awaitTermination(10, TimeUnit.SECONDS);
        Assertions.assertTrue(b);
    }

    @Test
    void testRun() {
        final List<IGameLoop> gameLoopList = gameLoopGroup.run(10, 10, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(gameLoopList.isEmpty());


        final List<IGameLoop> gameLoopList1 = gameLoopGroup.run(10, 10, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(gameLoopGroup.selectAll().length, gameLoopList1.size());
    }

    @Test
    void testAwaitTermination() {
        final List<IGameLoop> gameLoopList = gameLoopGroup.run(10, 10, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(gameLoopList.isEmpty());

        final Boolean aBoolean = Assertions.assertDoesNotThrow(() -> gameLoopGroup.awaitTermination(10, TimeUnit.SECONDS));
        Assertions.assertFalse(aBoolean);

        gameLoopGroup.shutdown();
        final Boolean aBoolean1 = Assertions.assertDoesNotThrow(() -> gameLoopGroup.awaitTermination(10, TimeUnit.SECONDS));
        Assertions.assertTrue(aBoolean1);
    }

    @Test
    void testSelectNext() {
        IGameLoop iGameLoop1 = gameLoopGroup.selectNext();
        Assertions.assertEquals(gameLoopGroup.getId() + "-1", iGameLoop1.getId());

        for (int i = 0; i < 1000000; i++) {
            final IGameLoop iGameLoop2 = gameLoopGroup.selectNext();
            Assertions.assertNotSame(iGameLoop2, iGameLoop1);
            iGameLoop1 = iGameLoop2;
        }
    }

    @Test
    void testSelectFunction() {
        final IGameLoop iGameLoop1 = gameLoopGroup.selectNext();
        final String id = UUID.randomUUID().toString();
        final CompletableFuture<Boolean> future = iGameLoop1.submit(IEntityManagerFunction.registerEntity(new Entity(id)));
        future.join();

        //选择实体数量最多的一个（实际业务中，是选取实体数量最少的一个，这里是为了方便测试）
        final List<IGameLoop> gameLoopList1 = gameLoopGroup.select(IEntityManagerFunction.getEntityCount(), Comparator.reverseOrder(), 1);
        Assertions.assertEquals(1, gameLoopList1.size());
        Assertions.assertSame(iGameLoop1, gameLoopList1.get(0));

        //选择实体所在的那个IGameLoop
        final List<IGameLoop> gameLoopList2 = gameLoopGroup.select(IEntityManagerFunction.hasEntity(id), Comparator.reverseOrder(), 1);
        Assertions.assertEquals(1, gameLoopList2.size());
        Assertions.assertSame(iGameLoop1, gameLoopList2.get(0));
    }

    @Test
    void testBenchmark() {

        gameLoopGroup.run(0, 20, TimeUnit.MILLISECONDS);

        final int entityCountBase = 100000;
        final int gameLoopCount = gameLoopGroup.selectAll().length;
        final int mod = entityCountBase % gameLoopCount;
        final int entityCount = Math.max(gameLoopCount, entityCountBase - mod);

        final List<CompletableFuture<Boolean>> submitFutureList = new ArrayList<>(entityCount);
        final Map<String, CompletableFuture<Boolean>> futureMap = new ConcurrentHashMap<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            final IGameLoop iGameLoop = gameLoopGroup.selectNext();
            final String id = UUID.randomUUID().toString();
            futureMap.put(id, new CompletableFuture<>());
            final Entity entity = new Entity(id) {
                @Override
                public void tick(long elapse) {
                    super.tick(elapse);

                    futureMap.get(getId()).complete(true);
                }
            };

            final CompletableFuture<Boolean> submit = iGameLoop.submit(IEntityManagerFunction.registerEntity(entity));
            submitFutureList.add(submit);
        }

        final long submitFailedCount = submitFutureList.stream()
                .parallel()
                .map(CompletableFuture::join)
                .filter(b -> !b.booleanValue())
                .count();

        Assertions.assertEquals(0, submitFailedCount);

        final long tickFailedcount = futureMap.values().stream()
                .parallel()
                .map(f -> {
                    try {
                        return f.get(100, TimeUnit.MILLISECONDS);
                    } catch (Throwable ignored) {
                    }
                    return false;
                })
                .filter(b -> !b.booleanValue())
                .count();

        Assertions.assertEquals(0, tickFailedcount);

        gameLoopGroup.shutdown();
        Assertions.assertDoesNotThrow(() -> gameLoopGroup.awaitTermination(10, TimeUnit.SECONDS));
    }



}
