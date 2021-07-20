package org.gamedo.ecs.interfaces;

import lombok.extern.slf4j.Slf4j;
import org.gamedo.configuration.GamedoConfiguration;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.EntityComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@Slf4j
@SpringBootTest(classes = GamedoConfiguration.class)
class IComponentTest {

    private final IEntity entity = new Entity(UUID.randomUUID().toString());
    private final IComponent<IEntity> component = new EntityComponent(entity);

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