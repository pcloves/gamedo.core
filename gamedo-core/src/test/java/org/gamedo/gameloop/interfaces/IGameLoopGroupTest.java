package org.gamedo.gameloop.interfaces;

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.gamedo.GameLoopGroupConfiguration;
import org.gamedo.annotation.Subscribe;
import org.gamedo.annotation.Tick;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.exception.GameLoopException;
import org.gamedo.gameloop.Category;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.util.Pair;
import org.gamedo.util.function.*;
import org.junit.jupiter.api.*;
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

    @BeforeEach
    void setUp() {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(GameLoopGroupConfiguration.class);
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

        Assertions.assertThrows(GameLoopException.class, () -> new GameLoopGroup("testGroup", 500, gameLoopList.toArray(IGameLoop[]::new)));
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
    void testConcurrentRegister() {

        final int gameLoopCount = 100;
        final String gameLoopIdPrefix = "testGameLoop-";

        final IGameLoop[] gameLoops = IntStream.rangeClosed(1, gameLoopCount)
                .mapToObj(i -> new GameLoop(gameLoopIdPrefix + i))
                .toArray(IGameLoop[]::new);
        final IGameLoopGroup gameLoopGroup = new GameLoopGroup("testGameLoopGroup", 0);

        Arrays.stream(gameLoops).parallel().forEach(gameLoopGroup::register);

        Assertions.assertEquals(gameLoopCount, gameLoopGroup.size());
    }

    @Test
    @DisplayName("多线程并发注册同一个gameLoop")
    void testConcurrentRegisterSameGameLoop() {
        final IGameLoopGroup gameLoopGroup = new GameLoopGroup("testGameLoopGroup", 0);
        final GameLoop gameLoop = new GameLoop("testGameLoop");

        IntStream.rangeClosed(1, Runtime.getRuntime().availableProcessors())
                .forEach(i -> gameLoopGroup.register(gameLoop));

        Assertions.assertEquals(1, gameLoopGroup.size());
    }

    @Test
    void testSelect() {
        final Map<String, GameLoop> gameLoopMap = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new GameLoop("test" + i))
                .collect(Collectors.toMap(IGameLoop::getId, Function.identity()));

        final GameLoopGroup gameLoopGroup1 = new GameLoopGroup("testGroup", 1, gameLoopMap.values().toArray(IGameLoop[]::new));

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
        gameLoopGroup.select(IGameLoopEntityManagerFunction.getEntityCount(Category.Entity), Comparator.reverseOrder(), 1)
                .thenApply(list -> list.get(0))
                .thenCompose(gameLoop -> gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity)))
                .thenAccept(r -> log.info("register finish, result:{}", r));

        //选择实体数量最多的一个（实际业务中，是选取实体数量最少的一个，这里是为了方便测试）
        final List<IGameLoop> gameLoopList1 = gameLoopGroup.select(IGameLoopEntityManagerFunction.getEntityCount(Category.Entity), Comparator.reverseOrder(), 1).join();
        Assertions.assertEquals(1, gameLoopList1.size());
        Assertions.assertSame(iGameLoop1, gameLoopList1.get(0));

        //选择实体所在的那个IGameLoop
        final List<IGameLoop> gameLoopList2 = gameLoopGroup.select(IGameLoopEntityManagerFunction.hasEntity(id, Category.Entity), Comparator.reverseOrder(), 1).join();
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
                .map(s -> gameLoopGroup.select(IGameLoopEntityManagerFunction.hasEntity(s, Category.Entity),
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
                .filter(b -> !b)
                .count();
        log.info("finish parallel join");

        Assertions.assertEquals(0, submitFailedCount);

        final long tickFailedCount = futureMap.values().stream()
                .parallel()
                .map(f -> {
                    try {
                        return f.get(100, TimeUnit.MILLISECONDS);
                    } catch (Throwable ignored) {
                    }
                    return false;
                })
                .filter(b -> !b)
                .count();

        log.info("finish tick");
        Assertions.assertEquals(0, tickFailedCount);

        gameLoopGroup.shutdown();
        Assertions.assertDoesNotThrow(() -> gameLoopGroup.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    void testSubmitFilter() {

        final IGameLoop iGameLoop = gameLoopGroup.selectNext();
        iGameLoop.submit(gameLoop -> gameLoop.addComponent(Object.class, new Object())).join();

        final EntityPredicate<IGameLoop> filter1 = IEntityFunction.hasComponent(Object.class);
        final EntityPredicate<IGameLoop> filter2 = IEntityFunction.hasComponent(IGameLoopEntityManager.class);
        final GameLoopFunction<Integer> function = IGameLoopEventBusFunction.post(EventTest.class, () -> new EventTest("test"));
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

        List<Integer> future = gameLoopGroup.submitAll(IGameLoopEventBusFunction.post(EventTest.class, () -> new EventTest("eventTest")))
                .exceptionally(throwable -> Collections.emptyList())
                .join();
        Assertions.assertEquals(iGameLoops.length, future.size());
        Assertions.assertEquals(iGameLoops.length, future.stream().filter(c -> c == 1).count());
    }

    @Test
    void selectHashing() {

        for (int i = 0; i < 100; i++) {
            final String hashKey = Long.valueOf(ThreadLocalRandom.current().nextLong()).toString();
            Assertions.assertEquals(gameLoopGroup.selectHashing(hashKey), gameLoopGroup.selectHashing(hashKey));
        }
    }

    @Test
    @DisplayName("测试一致性hash的分布")
    void selectHashingDistribution()
    {
        final GameLoopGroup gameLoopGroup = new GameLoopGroup("test", 600, Runtime.getRuntime().availableProcessors());
        final int gameLoopSize = gameLoopGroup.size();
        final int selectCountPerGameLoop = 100000;
        final Map<String, Integer> statisticMap = new HashMap<>(gameLoopSize);

        for (int i = 0; i < gameLoopSize * selectCountPerGameLoop; i++) {
            final IGameLoop gameLoop = gameLoopGroup.selectHashing(UUID.randomUUID().toString());
            statisticMap.compute(gameLoop.getId(), (id, count) -> count == null ? 1 : count + 1);
        }

        IntSummaryStatistics stats = statisticMap.values().stream().mapToInt( c -> c ).summaryStatistics();

        Assertions.assertEquals(statisticMap.size(), gameLoopGroup.size());
        Assertions.assertTrue(stats.getMin() > selectCountPerGameLoop * 0.9, () -> "stats:" + stats);
        Assertions.assertTrue(stats.getMax() < selectCountPerGameLoop * 1.1, () -> "stats:" + stats);

        final double sum = statisticMap.values().stream()
                .mapToDouble(integer -> Math.pow(integer - stats.getAverage(), 2))
                .sum();

        //总体标准偏差
        final double stdDev = Math.sqrt(sum / statisticMap.size());

        System.out.println("stdDev:" + stdDev);
    }

    @Test
    void selectHashingByNettyChannelId() {
        final List<String> channelList = Arrays.asList(
                "0126f2d4",
                "03bff0c0",
                "05cbb917",
                "0dc0b25a",
                "0e455fb3",
                "0f4aeb40",
                "105e9f02",
                "14b258c9",
                "1652d2c4",
                "1d40d7a6",
                "1f61eafd",
                "2300035b",
                "24d3b6d5",
                "25d329f9",
                "269b9ef7",
                "312ff6a8",
                "31a22322",
                "31e32f66",
                "37475f1e",
                "39dcf044",
                "3b88f7a3",
                "43177bb3",
                "44487c8c",
                "49dff97f",
                "509db2f7",
                "56d2dba8",
                "61fffe2c",
                "6473f7fd",
                "662ccdd2",
                "671613a0",
                "674a109e",
                "6cbcd35d",
                "6fe43c19",
                "72b1d380",
                "78eb71ec",
                "7f91c353",
                "8158da91",
                "818c1cdc",
                "8373964c",
                "882def47",
                "8d9fb0b7",
                "90900103",
                "91569452",
                "9bcf7199",
                "a08b98d7",
                "a7f6308d",
                "a88c5408",
                "aef324dc",
                "b5f080ad",
                "b9c66614",
                "bf5fe2d7",
                "c2c2b058",
                "c44cc68e",
                "c7aa7f21",
                "cb9917a0",
                "cbc510b7",
                "d54a2b83",
                "d5860411",
                "d7e1c7cf",
                "dc5f4310",
                "e2eb712b",
                "e3d3db56",
                "e6bfa907",
                "e93c1562",
                "e98c717d",
                "ecd5b097",
                "f904416c",
                "fadafa30",
                "fc15889e",
                "fc45f5ed"
        );

        final GameLoopGroup gameLoopGroup = new GameLoopGroup("test", 1000, 30);

        final Map<String, List<Pair<String, String>>> collect = channelList.stream()
                .map(s -> Pair.of(gameLoopGroup.selectHashing(s).getId(), s))
                .collect(Collectors.groupingBy(Pair::getK));

        //noinspection unused
        final Map<String, Integer> map = collect.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));

        final double average = (double) channelList.size() / gameLoopGroup.size();
        final double sum = map.values().stream()
                .mapToDouble(integer -> Math.pow(integer - average, 2))
                .sum();

        //总体标准偏差
        final double stdDev = Math.sqrt(sum / map.size());

        System.out.println(stdDev);
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
