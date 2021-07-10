package org.gamedo.ecs;

import lombok.extern.log4j.Log4j2;
import org.gamedo.configuration.GamedoConfiguration;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;

@Log4j2
@SpringBootTest(classes = GamedoConfiguration.class)
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
        Assertions.assertFalse(component.getInterface(IEntity.class).isPresent());

        Assertions.assertEquals(component.getInterface(IComponent.class).get(), component);
    }

    @Test
    public void testGetOwner() {
        Assertions.assertEquals(entity, component.getOwner());
    }
}