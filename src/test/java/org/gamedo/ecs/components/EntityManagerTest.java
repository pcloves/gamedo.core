package org.gamedo.ecs.components;

import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IEntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityManagerTest {
    private final IEntity entity = new Entity(UUID.randomUUID().toString());
    private IEntityManager entityMgr;

    @BeforeEach
    void setUp() {
        entityMgr = new EntityManager(null, entity, null);
    }

    @Test
    void testRegisterEntity() {

        final String entityId = UUID.randomUUID().toString();

        assertTrue(entityMgr.registerEntity(new Entity(entityId)));
        assertTrue(entityMgr.hasEntity(entityId));

        assertFalse(entityMgr.registerEntity(new Entity(entityId)));
    }

    @Test
    void testUnregisterEntityAndHasEntity() {

        final String entityId = UUID.randomUUID().toString();
        final Entity entity = new Entity(entityId);
        assertTrue(entityMgr.registerEntity(entity));
        assertTrue(entityMgr.hasEntity(entityId));

        assertEquals(entity, entityMgr.unregisterEntity(entityId).orElse(null));
        assertFalse(entityMgr.hasEntity(entityId));

        assertEquals(Optional.empty(), entityMgr.unregisterEntity(entityId));
    }

    @Test
    void testGetEntityMap() {

        final String entityId1 = UUID.randomUUID().toString();
        final String entityId2 = UUID.randomUUID().toString();
        final Entity entity1 = new Entity(entityId1);
        final Entity entity2 = new Entity(entityId2);
        assertTrue(entityMgr.registerEntity(entity1));
        assertTrue(entityMgr.registerEntity(entity2));

        final Map<String, IEntity> entityMap = entityMgr.getEntityMap();

        assertEquals(2, entityMap.size());
        assertThrows(UnsupportedOperationException.class, () -> entityMap.remove(entityId1));

        entityMgr.unregisterEntity(entityId1);
        assertEquals(1, entityMap.size());

        entityMgr.unregisterEntity(entityId2);
        assertEquals(0, entityMap.size());
    }
}