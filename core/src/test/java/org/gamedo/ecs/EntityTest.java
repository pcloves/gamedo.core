package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.Tick;
import org.gamedo.configuration.GamedoConfiguration;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.functions.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest(classes = GamedoConfiguration.class)
class EntityTest {

    private final ConfigurableApplicationContext context;

    EntityTest(ConfigurableApplicationContext context) {
        this.context = context;
    }
    @Test
    public void testCustomTick() {

        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final IGameLoop gameLoop = context.getBean(IGameLoop.class, UUID.randomUUID().toString());
        final Entity entity = new Entity(UUID.randomUUID().toString());
        entity.addComponent(My1SecondTickComponent.class, new My1SecondTickComponent(entity, future));

        gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity));

        final Boolean customTicked = assertDoesNotThrow(() -> future.get());
        assertTrue(customTicked);
    }

    private static class My1SecondTickComponent extends EntityComponent {
        private final CompletableFuture<Boolean> ticked;

        private My1SecondTickComponent(IEntity owner, CompletableFuture<Boolean> ticked) {
            super(owner);
            this.ticked = ticked;
        }

        @Tick(tick = 1, timeUnit = TimeUnit.SECONDS)
        private void tick1S(Long currentMilliSecond, Long lastTickMilliSecond) {
            ticked.complete(true);
        }
    }
}