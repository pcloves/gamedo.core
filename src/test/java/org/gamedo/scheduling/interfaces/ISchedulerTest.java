package org.gamedo.scheduling.interfaces;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IEntityManagerFunction;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.scheduling.CronScheduled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
class ISchedulerTest {

    private static final String CRON_SECONDLY_EXPRESSION = "*/1 * * * * *";
    private static final String CRON_10_SECONDLY_EXPRESSION = "*/10 * * * * *";
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
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(10));

        Assertions.assertEquals(10, component.value.get());
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

        final int sleepSecond = ThreadLocalRandom.current().nextInt(10, 31);
        log.info("begin sleep {} seconds", sleepSecond);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(sleepSecond));
        Assertions.assertEquals(sleepSecond / 10 + sleepSecond, object.value.get());
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
        final int expected = sleepSecond / 10 + sleepSecond;
        Assertions.assertEquals(expected, object.value.get());

        final CompletableFuture<Integer> future1 = iGameLoop.submit(ISchedulerFunction.unregisterSchedule(object.getClass()));
        final Integer result1 = Assertions.assertDoesNotThrow(() -> future1.get());
        log.info("unregisterSchedule finish.");
        Assertions.assertEquals(2, result1);

        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(10)));
        Assertions.assertEquals(expected, object.value.get());
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
    static class ScheduledSubObject extends ScheduledObject
    {
        @CronScheduled(CRON_10_SECONDLY_EXPRESSION)
        private void schedulePer10Second() {
            value.incrementAndGet();

            log.info("schedulePer10Second, thread:{}", Thread.currentThread().getName());
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