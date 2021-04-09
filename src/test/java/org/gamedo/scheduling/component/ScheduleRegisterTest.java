package org.gamedo.scheduling.component;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IEntityFunction;
import org.gamedo.ecs.interfaces.IEntityManagerFunction;
import org.gamedo.gameloop.GameLoopGroup;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.scheduling.CronScheduled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@SpringBootApplication
@EnableScheduling
@Log4j2
class ScheduleRegisterTest {

    private static final String CRON_SECONDLY_EXPRESSION = "*/1 * * * * *";
    private IGameLoopGroup gameLoopGroup;
    @Autowired
    private TaskScheduler taskScheduler;

    @BeforeEach
    void setUp() {
        gameLoopGroup = new GameLoopGroup("GameLoopGroup");

        Arrays.stream(gameLoopGroup.selectAll())
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

        final Entity entity = new Entity(UUID.randomUUID().toString());
        final MyScheduleComponent component = new MyScheduleComponent(entity);
        entity.addComponent(MyScheduleComponent.class, component);

        log.info("registerEntity begin");
        final CompletableFuture<Boolean> future = gameLoopGroup.selectNext().submit(IEntityManagerFunction.registerEntity(entity));

        final Boolean result = Assertions.assertDoesNotThrow(() -> future.get());
        log.info("registerEntity finish, result:{}", result);
        Assertions.assertDoesNotThrow(() -> TimeUnit.SECONDS.sleep(10));

        Assertions.assertEquals(10, component.value.get());
    }

    @Log4j2
    public static class MyScheduleComponent extends Component {

        private final AtomicInteger value = new AtomicInteger(0);

        public MyScheduleComponent(IEntity owner) {
            super(owner);
        }

        @CronScheduled(CRON_SECONDLY_EXPRESSION)
        private void scheduleSecondly() {
            value.incrementAndGet();
        }
    }
}