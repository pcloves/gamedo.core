package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.interfaces.IGameLoopEntityRegisterFunction;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
class EntityTest {

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
        final GameLoop gameLoop = new GameLoop(UUID.randomUUID().toString());
        final Entity entity = new Entity(UUID.randomUUID().toString()) {
            @Override
            public void tick(long elapse) {
                super.tick(elapse);

                futureGameLoop.complete(getBelongedGameLoop());
                future.complete(isInGameLoop());
            }
        };

        gameLoop.run(0, 10, TimeUnit.MICROSECONDS);
        gameLoop.submit(IGameLoopEntityRegisterFunction.registerEntity(entity));

        final Boolean inGameLoop = assertDoesNotThrow(() -> future.get());
        assertTrue(inGameLoop);

        final IGameLoop gameLoop1 = assertDoesNotThrow(() -> futureGameLoop.get().get());
        assertEquals(gameLoop, gameLoop1);

        final CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> entity.isInGameLoop());
        final Boolean inGameLoop1 = assertDoesNotThrow(() -> future1.get());
        assertFalse(inGameLoop1);

    }
}