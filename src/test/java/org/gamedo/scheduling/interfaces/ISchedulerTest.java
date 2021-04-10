package org.gamedo.scheduling.interfaces;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IEntityManagerFunction;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.scheduling.CronScheduled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
class ISchedulerTest {

    private static final String CRON_SECONDLY_EXPRESSION = "*/1 * * * * *";
    private static final String CRON_5_SECONDLY_EXPRESSION = "*/5 * * * * *";
    private static final String CRON_10_SECONDLY_EXPRESSION = "*/10 * * * * *";
    private static final String SCHEDULE_SECONDLY_METHOD_NAME = "scheduleSecondly";
    private static final String SCHEDULE_10_SECOND_METHOD_NAME = "schedulePer10Second";
    private static final String SCHEDULE_DYNAMIC_METHOD_NAME = "scheduleDynamic";
    private IGameLoopGroup gameLoopGroup;

    @BeforeEach
    void setUp() {
        gameLoopGroup = new GameLoopGroup("GameLoopGroup");
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        gameLoopGroup.shutdown();
        final boolean b = gameLoopGroup.awaitTermination(10, TimeUnit.SECONDS);
        Assertions.assertTrue(b);
    }

    @Test
    void testAutomaticSchedule() {

        final Entity entity = new Entity(UUID.randomUUID().toString());
        final ScheduledComponent component = new ScheduledComponent(entity);
        entity.addComponent(ScheduledComponent.class, component);

        log.info("registerEntity begin");
        final CompletableFuture<Boolean> future = gameLoopGroup.selectNext().submit(IEntityManagerFunction.registerEntity(entity));

        final Boolean result = Assertions.assertDoesNotThrow(() -> future.get());
        Assertions.assertTrue(result);
        log.info("registerEntity finish, result:{}", result);

        final int sleepSecond = ThreadLocalRandom.current().nextInt(10, 31);
        log.info("begin sleep {} seconds", sleepSecond);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond));

        Assertions.assertTrue(Math.abs(sleepSecond - component.value.get()) <= 1,
                () -> "expected:" + sleepSecond + ", actual:" + component.value.get());
    }

    @Test
    void testRegister() {

        final IGameLoop[] iGameLoops = gameLoopGroup.selectAll();
        final IGameLoop iGameLoop = iGameLoops[ThreadLocalRandom.current().nextInt(iGameLoops.length)];

        final ScheduledSubObject object = new ScheduledSubObject();
        final CompletableFuture<Integer> future = iGameLoop.submit(ISchedulerFunction.registerSchedule(object));
        final Integer result = Assertions.assertDoesNotThrow(() -> future.get());
        log.info("registerSchedule finish.");
        Assertions.assertEquals(2, result);

        final CompletableFuture<Integer> future1 = iGameLoop.submit(ISchedulerFunction.registerSchedule(object));
        final Integer result1 = Assertions.assertDoesNotThrow(() -> future1.get());
        log.info("registerSchedule finish.");
        Assertions.assertEquals(0, result1);

        final int sleepSecond = ThreadLocalRandom.current().nextInt(10, 31);
        log.info("begin sleep {} seconds", sleepSecond);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond));
        log.info("finish sleep");
        final int expected = sleepSecond / 10 + sleepSecond;
        Assertions.assertTrue(Math.abs(expected - object.value.get()) <= 1,
                () -> "expected:" + expected + ", actual:" + object.value.get());
    }

    @Test
    void testRegister1() {

        final IGameLoop[] iGameLoops = gameLoopGroup.selectAll();
        final IGameLoop iGameLoop = iGameLoops[ThreadLocalRandom.current().nextInt(iGameLoops.length)];

        final ScheduledSubObject object = new ScheduledSubObject();
        final CompletableFuture<Integer> future = iGameLoop.submit(ISchedulerFunction.registerSchedule(object));
        final Integer result = Assertions.assertDoesNotThrow(() -> future.get());
        log.info("registerSchedule finish.");
        Assertions.assertEquals(2, result);

        final int sleepSecond = ThreadLocalRandom.current().nextInt(10, 31);
        log.info("begin sleep {} seconds", sleepSecond);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond));
        log.info("finish sleep");
        final int expected = sleepSecond / 10 + sleepSecond;
        Assertions.assertTrue(Math.abs(expected - object.value.get()) <= 1);

        final Predicate<Method> predicate = method -> SCHEDULE_DYNAMIC_METHOD_NAME.equals(method.getName());
        final List<Method> methods = ReflectionUtils.findMethods(object.getClass(), predicate);
        Assertions.assertEquals(1, methods.size());

        final Method method = methods.get(0);
        final GameLoopFunction<Boolean> function = ISchedulerFunction.registerSchedule(object, method, CRON_5_SECONDLY_EXPRESSION);
        final CompletableFuture<Boolean> future1 = iGameLoop.submit(function);
        final boolean result1 = Assertions.assertDoesNotThrow(() -> future1.get());
        log.info("registerSchedule method {} using {} dynamiclly finish.", method, CRON_5_SECONDLY_EXPRESSION);
        Assertions.assertTrue(result1);

        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(6));
        Assertions.assertTrue(Math.abs(1 - object.valueDynamic.get()) <= 1,
                () -> "expected:" + expected + ", actual:" + object.value.get());
    }

    @Test
    void testUnregister() {

        final IGameLoop[] iGameLoops = gameLoopGroup.selectAll();
        final IGameLoop iGameLoop = iGameLoops[ThreadLocalRandom.current().nextInt(iGameLoops.length)];

        final ScheduledSubObject object = new ScheduledSubObject();
        final CompletableFuture<Integer> future = iGameLoop.submit(ISchedulerFunction.registerSchedule(object));
        final Integer result = Assertions.assertDoesNotThrow(() -> future.get());
        log.info("registerSchedule finish.");
        Assertions.assertEquals(2, result);

        final int sleepSecond = ThreadLocalRandom.current().nextInt(10, 31);
        log.info("begin sleep {} seconds", sleepSecond);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond));
        log.info("finish sleep");
        final int expected = sleepSecond / 10 + sleepSecond;
        Assertions.assertTrue(Math.abs(expected - object.value.get()) <= 1,
                () -> "expected:" + expected + ", actual:" + object.value.get());

        final CompletableFuture<Integer> future1 = iGameLoop.submit(ISchedulerFunction.unregisterSchedule(object.getClass()));
        final Integer result1 = Assertions.assertDoesNotThrow(() -> future1.get());
        log.info("unregisterSchedule finish.");
        Assertions.assertEquals(2, result1);

        final int sleepSecond1 = ThreadLocalRandom.current().nextInt(10);
        log.info("begin sleep {} seconds", sleepSecond1);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond1));
        log.info("finish sleep");

        Assertions.assertTrue(Math.abs(expected - object.value.get()) <= 1,
                () -> "expected:" + expected + ", actual:" + object.value.get());
    }

    @Test
    void testUnregister1() {

        final IGameLoop[] iGameLoops = gameLoopGroup.selectAll();
        final IGameLoop iGameLoop = iGameLoops[ThreadLocalRandom.current().nextInt(iGameLoops.length)];

        final ScheduledSubObject object = new ScheduledSubObject();
        final CompletableFuture<Integer> future = iGameLoop.submit(ISchedulerFunction.registerSchedule(object));
        final Integer result = Assertions.assertDoesNotThrow(() -> future.get());
        log.info("registerSchedule finish.");
        Assertions.assertEquals(2, result);

        final int sleepSecond = ThreadLocalRandom.current().nextInt(10, 31);
        log.info("begin sleep {} seconds", sleepSecond);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond));
        log.info("finish sleep");
        final int expected = sleepSecond / 10 + sleepSecond;
        Assertions.assertTrue(Math.abs(expected - object.value.get()) <= 1);

        final Predicate<Method> predicate = method -> SCHEDULE_10_SECOND_METHOD_NAME.equals(method.getName());
        final List<Method> methods = ReflectionUtils.findMethods(object.getClass(), predicate);
        Assertions.assertEquals(1, methods.size());

        final GameLoopFunction<Boolean> function = ISchedulerFunction.unregisterSchedule(object.getClass(), methods.get(0));
        final CompletableFuture<Boolean> future1 = iGameLoop.submit(function);
        final Boolean result1 = Assertions.assertDoesNotThrow(() -> future1.get());
        log.info("unregisterSchedule {} finish.", SCHEDULE_10_SECOND_METHOD_NAME);
        Assertions.assertTrue(result1);

        final int sleepSecond1 = ThreadLocalRandom.current().nextInt(10);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond1));
        Assertions.assertTrue(Math.abs(expected + sleepSecond1 - object.value.get()) <= 1,
                () -> "expected:" + expected + ", actual:" + object.value.get());
    }

    @Test
    void testUnregister2() {

        final IGameLoop[] iGameLoops = gameLoopGroup.selectAll();
        final IGameLoop iGameLoop = iGameLoops[ThreadLocalRandom.current().nextInt(iGameLoops.length)];

        final List<ScheduledSubObject> scheduledSubObjectList = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> new ScheduledSubObject())
                .collect(Collectors.toList());

        final List<CompletableFuture<Integer>> completableFutureList = scheduledSubObjectList.stream()
                .map(scheduledSubObject -> iGameLoop.submit(ISchedulerFunction.registerSchedule(scheduledSubObject)))
                .collect(Collectors.toList());

        final List<Integer> failedList = completableFutureList.stream()
                .map(CompletableFuture::join)
                .filter(integer -> integer != 2)
                .collect(Collectors.toList());

        Assertions.assertTrue(failedList.isEmpty());

        final int sleepSecond = ThreadLocalRandom.current().nextInt(10, 31);
        log.info("begin sleep {} seconds", sleepSecond);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond));
        log.info("finish sleep");
        final int expected = sleepSecond / 10 + sleepSecond;

        final List<Integer> failedValueList = scheduledSubObjectList.stream()
                .map(scheduledSubObject -> scheduledSubObject.value.get())
                .filter(value -> Math.abs(value - expected) >= 2)
                .collect(Collectors.toList());

        Assertions.assertTrue(failedValueList.isEmpty());
    }


    @SuppressWarnings("unused")
    static class ScheduledObject {
        final AtomicInteger value = new AtomicInteger(0);

        @CronScheduled(CRON_SECONDLY_EXPRESSION)
        private void scheduleSecondly() {
            value.incrementAndGet();

            log.info("scheduleSecondly, thread:{}", Thread.currentThread().getName());
        }
    }

    @SuppressWarnings("unused")
    static class ScheduledSubObject extends ScheduledObject {
        final AtomicInteger valueDynamic = new AtomicInteger(0);

        @CronScheduled(CRON_10_SECONDLY_EXPRESSION)
        private void schedulePer10Second() {
            value.incrementAndGet();

            log.info("schedulePer10Second, thread:{}", Thread.currentThread().getName());
        }

        void scheduleDynamic() {
            valueDynamic.incrementAndGet();
            log.info("scheduleDynamic, thread:{}", Thread.currentThread().getName());
        }
    }

    @SuppressWarnings("unused")
    static class ScheduledComponent extends Component {

        private final AtomicInteger value = new AtomicInteger(0);

        ScheduledComponent(IEntity owner) {
            super(owner);
        }

        @CronScheduled(CRON_SECONDLY_EXPRESSION)
        private void scheduleSecondly() {
            value.incrementAndGet();

            log.info("MyScheduleComponent, thread:{}", Thread.currentThread().getName());
        }
    }
}