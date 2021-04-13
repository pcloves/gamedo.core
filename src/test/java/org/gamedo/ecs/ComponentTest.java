package org.gamedo.ecs;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IEntityManagerFunction;
import org.gamedo.ecs.interfaces.ITickable;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class ComponentTest {

    private final IGameLoop iGameLoop = new GameLoop(UUID.randomUUID().toString());
    private final IEntity entity = new Entity(UUID.randomUUID().toString());
    private final IComponent component = new Component(entity);

    @Test
    public void testQueryInterface() {
        Assertions.assertTrue(component.getInterface(IComponent.class).isPresent());
        Assertions.assertTrue(component.getInterface(ITickable.class).isPresent());
        Assertions.assertFalse(component.getInterface(IEntity.class).isPresent());

        Assertions.assertEquals(component.getInterface(IComponent.class).get(), component);
        Assertions.assertEquals(component.getInterface(ITickable.class).get(), component);
    }

    @Test
    public void testGetOwner() {
        Assertions.assertEquals(entity, component.getOwner());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void testGetBelongedGameLoop() {

        Assertions.assertEquals(Optional.empty(), component.getOwnerBelongedGameLoop());

        final CompletableFuture<Boolean> future = iGameLoop.submit(IEntityManagerFunction.registerEntity(entity));
        future.join();

        Assertions.assertEquals(iGameLoop, component.getOwnerBelongedGameLoop().get());
    }
}