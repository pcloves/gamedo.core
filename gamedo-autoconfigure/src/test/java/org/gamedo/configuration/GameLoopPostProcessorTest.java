package org.gamedo.configuration;

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.gamedo.ApplicationBase;
import org.gamedo.annotation.*;
import org.gamedo.gameloop.GameLoopConfig;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.logging.Markers;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused"})
@SpringBootTest
@Log4j2
@Disabled
class GameLoopPostProcessorTest {

    private final ValidObject validObject;
    private final InvalidObject invalidObject;

    GameLoopPostProcessorTest(ApplicationContext applicationContext) {

        this.validObject = ApplicationBase.context().getBean(ValidObject.class);
        this.invalidObject = ApplicationBase.context().getBean(InvalidObject.class);
    }

    @Test
    void testSubscribeOn() {

        final GameLoopProperties gameLoopProperties = ApplicationBase.context().getBean(GameLoopProperties.class);
        final GameLoopConfig gameLoopConfig = gameLoopProperties.getIo().convert();
        //线程id："io-2"
        final IGameLoop gameLoop = ApplicationBase.context().getBean(IGameLoopGroup.class, gameLoopConfig).selectAll()[1];
        final int value = ThreadLocalRandom.current().nextInt();

        final CompletableFuture<Integer> future = gameLoop.submit(IGameLoopEventBusFunction.post(TestEvent.class, () -> new TestEvent(value)));

        future.join();

        Assertions.assertEquals(value, validObject.subscribeValue.get());
        Assertions.assertEquals(0, invalidObject.subscribeValue.get());
    }

    @Test
    void testTickOn() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);

        Assertions.assertNotEquals(0, validObject.tickValue.get());
        Assertions.assertEquals(0, invalidObject.tickValue.get());
    }

    @Test
    void testCronOn() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);

        Assertions.assertNotEquals(0, validObject.cronValue.get());
        Assertions.assertEquals(0, invalidObject.cronValue.get());
    }

    public static class TestObject
    {
        protected final AtomicInteger subscribeValue = new AtomicInteger(0);
        protected final AtomicInteger tickValue = new AtomicInteger(0);
        protected final AtomicInteger cronValue = new AtomicInteger(0);

        @Subscribe
        private void testEvent(TestEvent testEvent)
        {
            this.subscribeValue.set(testEvent.value);
        }

        @Tick(delay = 0, tick = 1, timeUnit = TimeUnit.SECONDS)
        private void tick(Long currentMilliSecond, Long lastMilliSecond)
        {
           log.info(Markers.GameLoopTest, "tick value:{}", tickValue.incrementAndGet());
        }

        @Cron("*/1 * * * * *")
        private void cron(Long currentTime, Long lastTriggerTime)
        {
            log.info(Markers.GameLoopTest, "cron value:{}",cronValue.incrementAndGet());
        }
    }

    /**
     * 这是一个正确的注解类，父类的3个方法都会触发
     */
    @SubscribeOn(gameLoopId = "io-2")
    @TickOn(gameLoopId = "single-1")
    @CronOn
    @Component
    public static class ValidObject extends TestObject
    {
    }

    /**
     * 这个类的注解都是错的，因此父类上的3个方法都不会触发
     */
    @SubscribeOn(gameLoopId = "invalid-1")
    @TickOn(gameLoopId = "invalid-1")
    @CronOn(gameLoopId = "invalid-1")
    @Component
    public static class InvalidObject extends TestObject
    {
    }

    @Value
    public static class TestEvent implements IEvent
    {
        int value;
    }
}