package org.gamedo.ecs;

import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.interfaces.IEntityManagerFunction;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class EntityTest {

    @Test
    public void testNotInGameLoop() {
        final Entity entity = new Entity(UUID.randomUUID().toString());

        assertEquals(Optional.empty(), entity.gameLoop());
    }

    @Test
    public void testInGameLoop() {

        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final GameLoop gameLoop = new GameLoop(UUID.randomUUID().toString());
        final Entity entity = new Entity(UUID.randomUUID().toString()) {
            @Override
            public void tick(long elapse) {
                super.tick(elapse);

                try {
                    @SuppressWarnings("OptionalGetWithoutIsPresent")
                    final IGameLoop iGameLoop = assertDoesNotThrow(() -> gameLoop().get());
                    assertEquals(iGameLoop, gameLoop);
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }

                future.complete(true);
            }
        };

        gameLoop.run(0, 10, TimeUnit.MICROSECONDS);
        gameLoop.submit(IEntityManagerFunction.registerEntity(entity));

        assertDoesNotThrow(() -> future.get());
    }
}