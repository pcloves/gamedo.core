package org.gamedo.ecs.interfaces;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.EntityComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@Log4j2
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