package org.gamedo.ecs.interfaces;

import org.gamedo.ecs.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IEntityTest {
    private static final String id = "IEntityTest";
    private IEntity entity;
    @BeforeEach
    void setUp() {
        entity = new Entity(id);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getId() {
        assertEquals(id, entity.getId());
    }

    @Test
    void hasComponent() {
        final boolean hasComponent1 = entity.hasComponent(Object.class);
        assertFalse(hasComponent1);

        entity.addComponent(Object.class, new Object());
        final boolean hasComponent2 = entity.hasComponent(Object.class);
        assertTrue(hasComponent2);
    }

    @Test
    void getComponent() {
        final Optional<Object> component1 = entity.getComponent(Object.class);
        assertTrue(component1.isEmpty());

        entity.addComponent(Object.class, new Object());
        final Optional<Object> component2 = entity.getComponent(Object.class);
        assertTrue(component2.isPresent());
    }

    @Test
    void getComponentMap() {
        final Map<Class<?>, Object> componentMap1 = entity.getComponentMap();
        assertEquals(0, componentMap1.size());

        entity.addComponent(Object.class, new Object());
        assertEquals(1, componentMap1.size());

        final Map<Class<?>, Object> componentMap2 = entity.getComponentMap();
        assertEquals(1, componentMap2.size());

        assertThrows(UnsupportedOperationException.class, () -> componentMap2.remove(Object.class));
    }

    @Test
    void addComponent() {

        final Object object1 = new Object();
        final boolean result1 = entity.addComponent(Object.class, object1);
        assertTrue(result1);

        final boolean result2 = entity.addComponent(Object.class, new Object());
        assertFalse(result2);

    }

    @Test
    void removeComponent() {
        final Object object = new Object();
        entity.addComponent(Object.class, object);

        final Optional<Object> removeComponent = entity.removeComponent(Object.class);

        assertFalse(removeComponent.isEmpty());
        assertEquals(object, removeComponent.get());
    }
}