package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.configuration.EnableGamedoApplication;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IGameLoopEntityManagerFunction;
import org.gamedo.ecs.interfaces.ITickable;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Log4j2
@ExtendWith(SpringExtension.class)
@EnableGamedoApplication
class ComponentTest {

    private final ConfigurableApplicationContext context;
    private final IGameLoop iGameLoop;
    private final IEntity entity = new Entity(UUID.randomUUID().toString());
    private final IComponent<IEntity> component = new EntityComponent(entity);

    ComponentTest(ConfigurableApplicationContext context) {
        this.context = context;
        iGameLoop = context.getBean(IGameLoop.class, UUID.randomUUID().toString());
    }

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

        final CompletableFuture<Boolean> future = iGameLoop.submit(IGameLoopEntityManagerFunction.registerEntity(entity));
        future.join();

        Assertions.assertEquals(iGameLoop, component.getOwnerBelongedGameLoop().get());
    }
}