package org.gamedo.gameloop.components.entitymanager.interfaces;

import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IGameLoopEntityManagerTest {
    private final IGameLoop gameLoop = Mockito.spy(new GameLoop("IGameLoopEntityManagerTest"));
    private IGameLoopEntityManager entityMgr;

    IGameLoopEntityManagerTest() {
        Mockito.when(gameLoop.inThread()).thenReturn(true);
    }

    @BeforeEach
    void setUp() {
        entityMgr = new GameLoopEntityManager(gameLoop);
    }

    @SuppressWarnings("unused")
    @Test
    void testRegisterEntity() {

        final String entityId1 = UUID.randomUUID().toString();

        assertTrue(entityMgr.registerEntity(new Entity(entityId1)));
        assertTrue(entityMgr.hasEntity(entityId1));

        final Optional<IPlayerEntity> entity = entityMgr.getEntity(entityId1);
        assertTrue(entity.isPresent());
        assertThrows(ClassCastException.class, () -> {
            final IPlayerEntity iPlayerEntity = entity.get();
        });
        assertFalse(entityMgr.registerEntity(new Entity(entityId1)));

        final String entityId2 = "player";
        assertTrue(entityMgr.registerEntity(new PlayerEntity(entityId2)));
        assertTrue(entityMgr.hasEntity(entityId2));

        final Optional<IEntity> entity1 = entityMgr.getEntity(entityId2);
        assertTrue(entity1.isPresent());
        assertDoesNotThrow(() -> {
            final IEntity iEntity = entity1.get();
            final IEntity iEntityPlayer = entity1.get();
        });
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

    @SuppressWarnings("unused")
    interface IPlayerEntity extends IEntity {
        String getRoleId();
    }

    public static class PlayerEntity extends Entity implements IPlayerEntity {
        public PlayerEntity(String id) {
            super(id);
        }

        @Override
        public String getRoleId() {
            return id;
        }
    }
}