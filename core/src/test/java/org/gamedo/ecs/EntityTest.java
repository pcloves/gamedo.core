package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.Tick;
import org.gamedo.configuration.GamedoConfiguration;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManagerFunction;
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
    public void testNotInGameLoop() {
        final Entity entity = new Entity(UUID.randomUUID().toString());

        assertFalse(entity.hasRegistered());
    }

    @Test
    public void testInGameLoop() {

        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final IGameLoop gameLoop = context.getBean(IGameLoop.class, UUID.randomUUID().toString());
        final Entity entity = new MyEntity(future);

        gameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity));

        final Boolean inGameLoop = assertDoesNotThrow(() -> future.get());
        assertTrue(inGameLoop);

        final CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> entity.hasRegistered(), gameLoop);
        final Boolean inGameLoop1 = assertDoesNotThrow(() -> future1.get());
        assertTrue(inGameLoop1);
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

    private static class MyEntity extends Entity {
        private final CompletableFuture<Boolean> future;

        private MyEntity( CompletableFuture<Boolean> future) {
            super(UUID.randomUUID().toString());
            this.future = future;
        }

        @Tick
        public void myTick(Long currentMilliSecond, Long lastTickMilliSecond) {
            future.complete(hasRegistered());
        }
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