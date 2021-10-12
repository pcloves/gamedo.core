package org.gamedo.gameloop.interfaces;

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.gamedo.GameLoopGroupConfiguration;
import org.gamedo.annotation.Subscribe;
import org.gamedo.annotation.Tick;
import org.gamedo.ecs.Entity;
import org.gamedo.util.function.IEntityFunction;
import org.gamedo.util.function.EntityPredicate;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.gamedo.util.function.GameLoopFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
class IGameLoopGroupTest {

    private IGameLoopGroup gameLoopGroup;
    private ConfigurableApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(GameLoopGroupConfiguration.class);
        gameLoopGroup = context.getBean(IGameLoopGroup.class);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        gameLoopGroup.shutdown();
        final boolean b = gameLoopGroup.awaitTermination(10, TimeUnit.SECONDS);
        Assertions.assertTrue(b);
    }

    @Test
    void testDuplicateCheck() {

        final List<GameLoop> gameLoopList = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new GameLoop("test" + i))
                .collect(Collectors.toList());

        final int i = ThreadLocalRandom.current().nextInt(gameLoopList.size()) + 1;
        gameLoopList.add(new GameLoop("test" + i));

        Assertions.assertThrows(GameLoopException.class, () -> new GameLoopGroup("testGroup", gameLoopList.toArray(IGameLoop[]::new)));
    }

    @Test
    void testSize() {
        final GameLoopGroup gameLoopGroup1 = new GameLoopGroup("test", 1);
        Assertions.assertEquals(1, gameLoopGroup1.size());

        final int gameLoopCount = Runtime.getRuntime().availableProcessors();
        final GameLoopGroup gameLoopGroup2 = new GameLoopGroup("test", gameLoopCount);
        Assertions.assertEquals(gameLoopCount, gameLoopGroup2.size());
    }

    @Test
    void testRegister() {

        final int gameLoopSizeBase = Runtime.getRuntime().availableProcessors();
        for (int j = 0; j < 1000; j++) {
            final IGameLoop gameLoop = new GameLoop("test-" + j);
            final int gameLoopSize = gameLoopGroup.size();
            final boolean register = gameLoopGroup.register(gameLoop);
            Assertions.assertTrue(register);
            Assertions.assertEquals(gameLoopSizeBase + j, gameLoopSize);

            for (int i = 0; i < gameLoopSize; i++) {
                gameLoopGroup.selectNext();
            }

            //循环一圈后，再次select才能选到自己
            Assertions.assertEquals(gameLoop, gameLoopGroup.selectNext());
        }
    }

    @Test
    void testSelect() {
        final Map<String, GameLoop> gameLoopMap = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new GameLoop("test" + i))
                .collect(Collectors.toMap(IGameLoop::getId, Function.identity()));

        final GameLoopGroup gameLoopGroup1 = new GameLoopGroup("testGroup", gameLoopMap.values().toArray(IGameLoop[]::new));

        gameLoopMap.forEach((key, value) -> Assertions.assertEquals(Optional.of(value), gameLoopGroup1.select(key)));
    }

    @Test
    void testSelectNext() {
        IGameLoop iGameLoop1 = gameLoopGroup.selectNext();
        final IGameLoop[] iGameLoops = gameLoopGroup.selectAll();
        Assertions.assertEquals(iGameLoops[0].getId(), iGameLoop1.getId());

        for (int i = 0; i < 1000000; i++) {
            final IGameLoop iGameLoop2 = gameLoopGroup.selectNext();
            Assertions.assertNotSame(iGameLoop2, iGameLoop1);
            iGameLoop1 = iGameLoop2;
        }
    }

    @Test
    void testSelectChooser() {
        final IGameLoop iGameLoop1 = gameLoopGroup.selectNext();
        final String id = UUID.randomUUID().toString();
        final CompletableFuture<Boolean> future = iGameLoop1.submit(IGameLoopEntityManagerFunction.registerEntity(new Entity(id)));
        future.join();

        final IEntity entity = new Entity("entity-1");
        gameLoopGroup.select(IGameLoopEntityManagerFunction.getEntityCount(), Comparator.reverseOrder(), 1)
                .thenApply(list -> list.get(0))
                .thenCompose(gameLoop -> gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity)))
                .thenAccept(r -> log.info("register finish, result:{}", r));

        //选择实体数量最多的一个（实际业务中，是选取实体数量最少的一个，这里是为了方便测试）
        final List<IGameLoop> gameLoopList1 = gameLoopGroup.select(IGameLoopEntityManagerFunction.getEntityCount(), Comparator.reverseOrder(), 1).join();
        Assertions.assertEquals(1, gameLoopList1.size());
        Assertions.assertSame(iGameLoop1, gameLoopList1.get(0));

        //选择实体所在的那个IGameLoop
        final List<IGameLoop> gameLoopList2 = gameLoopGroup.select(IGameLoopEntityManagerFunction.hasEntity(id), Comparator.reverseOrder(), 1).join();
        Assertions.assertEquals(1, gameLoopList2.size());
        Assertions.assertSame(iGameLoop1, gameLoopList2.get(0));
    }

    @Test
    void testSelectFilter() {

        final HashSet<String> entityIdSet = new HashSet<>(Arrays.asList("a", "b", "c"));
        final List<Boolean> collect = entityIdSet.stream()
                .parallel()
                .map(s -> gameLoopGroup.selectNext().submit(IGameLoopEntityManagerFunction.registerEntity(new Entity(s))).join())
                .distinct()
                .collect(Collectors.toList());

        Assertions.assertEquals(1, collect.size());
        Assertions.assertEquals(true, collect.get(0));

        final List<CompletableFuture<List<IGameLoop>>> futureList = entityIdSet.stream()
                .map(s -> gameLoopGroup.select(IGameLoopEntityManagerFunction.hasEntity(s),
                        Comparator.reverseOrder(),
                        1))
                .collect(Collectors.toList());
        final CompletableFuture<List<IGameLoop>> future = CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new))
                .thenApply(r -> futureList.stream()
                        .flatMap(t -> t.join().stream())
                        .distinct()
                        .collect(Collectors.toList()));

        final Object[] actual = Arrays.stream(gameLoopGroup.selectAll()).limit(3).toArray();
        final Object[] expected = future.join().toArray();
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    void testBenchmark() {

        final int entityCountBase = 10000;
        final int size = gameLoopGroup.size();
        final int mod = entityCountBase % size;
        final int entityCount = Math.max(size, entityCountBase - mod);

        log.info("gameLoop count:{}", size);
        log.info("entity count:{}", entityCount);
        final List<CompletableFuture<Boolean>> submitFutureList = new ArrayList<>(entityCount);
        final Map<String, CompletableFuture<Boolean>> futureMap = new ConcurrentHashMap<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            final IGameLoop iGameLoop = gameLoopGroup.selectNext();
            final String id = UUID.randomUUID().toString();
            futureMap.put(id, new CompletableFuture<>());
            final Entity entity = new MyEntity(id, futureMap);

            final CompletableFuture<Boolean> submit = iGameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity));
            submitFutureList.add(submit);
        }

        log.info("begin parallel join");
        final long submitFailedCount = submitFutureList.stream()
                .parallel()
                .map(CompletableFuture::join)
                .filter(b -> !b.booleanValue())
                .count();
        log.info("finish parallel join");

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

        log.info("finish tick");
        Assertions.assertEquals(0, tickFailedcount);

        gameLoopGroup.shutdown();
        Assertions.assertDoesNotThrow(() -> gameLoopGroup.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    void testSubmitFilter() {

        final IGameLoop iGameLoop = gameLoopGroup.selectNext();
        iGameLoop.submit(gameLoop -> gameLoop.addComponent(Object.class, new Object())).join();

        final EntityPredicate<IGameLoop> filter1 = IEntityFunction.hasComponent(Object.class);
        final EntityPredicate<IGameLoop> filter2 = IEntityFunction.hasComponent(IGameLoopEntityManager.class);
        final GameLoopFunction<Integer> function = IGameLoopEventBusFunction.post(new EventTest("test"));
        final List<Integer> list1 = gameLoopGroup.submit(EntityPredicate.And(filter1, filter2), function).join();

        Assertions.assertEquals(1, list1.size());


        final EntityPredicate<IGameLoop> filter3 = IEntityFunction.<IGameLoop>hasComponent(Object.class)
                .and(IEntityFunction.hasComponent(IGameLoopEntityManager.class));
        final List<Integer> list2 = gameLoopGroup.submit(filter3, function).join();
        Assertions.assertEquals(1, list2.size());

        final EntityPredicate<IGameLoop> filter4 = entity -> entity.hasComponent(Object.class);
        final EntityPredicate<IGameLoop> filter5 = entity -> entity.hasComponent(IGameLoopEntityManager.class);
        final List<Integer> list3 = gameLoopGroup.submit(EntityPredicate.And(filter4, filter5), function).join();
        Assertions.assertEquals(1, list3.size());

        final EntityPredicate<IGameLoop> filter6 = EntityPredicate.<IGameLoop>True()
                .and(entity -> entity.hasComponent(Object.class))
                .and(entity -> entity.hasComponent(IGameLoopEntityManager.class));

        final List<Integer> list4 = gameLoopGroup.submit(filter6, function).join();
        Assertions.assertEquals(1, list4.size());
    }

    @Test
    void testSubmitAll() {

        final IGameLoop[] iGameLoops = gameLoopGroup.selectAll();
        IntStream.rangeClosed(1, iGameLoops.length)
                .forEach(i -> iGameLoops[i - 1].submit(IGameLoopEntityManagerFunction.registerEntity(new EntityTestSubmitAll("entity-" + i))));

        List<Integer> future = gameLoopGroup.submitAll(IGameLoopEventBusFunction.post(new EventTest("eventTest")))
                .exceptionally(throwable -> Collections.emptyList())
                .join();
        Assertions.assertEquals(iGameLoops.length, future.size());
        Assertions.assertEquals(iGameLoops.length, future.stream().filter(c -> c == 1).count());
    }

    @Test
    void selectHashing() {

        for (int i = 0; i < 100; i++) {
            final Object object2Hash = Long.valueOf(ThreadLocalRandom.current().nextLong());
            Assertions.assertEquals(gameLoopGroup.selectHashing(object2Hash), gameLoopGroup.selectHashing(object2Hash));
        }
    }

    @Value
    private static class EventTest implements IEvent {
        String eventName;
    }

    @SuppressWarnings("unused")
    private static class MyEntity extends Entity {
        private final Map<String, CompletableFuture<Boolean>> futureMap;

        private MyEntity(String id, Map<String, CompletableFuture<Boolean>> futureMap) {
            super(id);
            this.futureMap = futureMap;
        }

        @Tick(tick = 10)
        public void myTick(Long currentMilliSecond, Long lastMilliSecond) {
            futureMap.get(getId()).complete(true);
        }


    }

    @SuppressWarnings("unused")
    private static class EntityTestSubmitAll extends Entity {
        private EntityTestSubmitAll(String id) {
            super(id);
        }

        @Subscribe
        private void onEventTest(EventTest event) {
            log.info("[onEventTest]EventTest, entityId:{}, event:{}", getId(), event.getEventName());
        }
    }
}
