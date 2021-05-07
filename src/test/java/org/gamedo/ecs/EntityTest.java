package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.configuration.EnableGamedoApplication;
import org.gamedo.ecs.interfaces.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@ExtendWith(SpringExtension.class)
@EnableGamedoApplication
class EntityTest {

    private final ConfigurableApplicationContext context;

    EntityTest(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Test
    public void testNotInGameLoop() {
        final Entity entity = new Entity(UUID.randomUUID().toString());

        assertFalse(entity.isInGameLoop());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void testInGameLoop() {

        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CompletableFuture<Optional<IGameLoop>> futureGameLoop = new CompletableFuture<>();
        final IGameLoop gameLoop = context.getBean(IGameLoop.class, UUID.randomUUID().toString());
        final Entity entity = new MyEntity(futureGameLoop, future);

        gameLoop.run(0, 10, TimeUnit.MICROSECONDS);
        gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity));

        final Boolean inGameLoop = assertDoesNotThrow(() -> future.get());
        assertTrue(inGameLoop);

        final IGameLoop gameLoop1 = assertDoesNotThrow(() -> futureGameLoop.get().get());
        assertEquals(gameLoop, gameLoop1);

        final CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> entity.isInGameLoop());
        final Boolean inGameLoop1 = assertDoesNotThrow(() -> future1.get());
        assertFalse(inGameLoop1);

    }

    private static class MyEntity extends Entity {
        private final CompletableFuture<Optional<IGameLoop>> futureGameLoop;
        private final CompletableFuture<Boolean> future;

        private MyEntity(CompletableFuture<Optional<IGameLoop>> futureGameLoop, CompletableFuture<Boolean> future) {
            super(UUID.randomUUID().toString());
            this.futureGameLoop = futureGameLoop;
            this.future = future;
        }

        @Override
        public void tick(long elapse) {
            super.tick(elapse);

            futureGameLoop.complete(getBelongedGameLoop());
            future.complete(isInGameLoop());
        }
    }
}