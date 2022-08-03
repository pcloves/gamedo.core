package org.gamedo.gameloop.components.entitymanager.interfaces;

import lombok.Getter;
import org.gamedo.annotation.Subscribe;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.event.EventRegisterEntityPost;
import org.gamedo.event.EventRegisterEntityPre;
import org.gamedo.event.EventUnregisterEntityPost;
import org.gamedo.event.EventUnregisterEntityPre;
import org.gamedo.gameloop.Category;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
        assertTrue(entityMgr.hasEntity(entityId1, Category.Entity));

        final Optional<IPlayerEntity> entity = entityMgr.getEntity(entityId1, Category.Entity);
        assertTrue(entity.isPresent());
        assertThrows(ClassCastException.class, () -> {
            final IPlayerEntity iPlayerEntity = entity.get();
        });
        assertFalse(entityMgr.registerEntity(new Entity(entityId1)));

        final String entityId2 = "player";
        assertTrue(entityMgr.registerEntity(new PlayerEntity(entityId2)));
        assertTrue(entityMgr.hasEntity(entityId2, Category.Entity));

        final Optional<IEntity> entity1 = entityMgr.getEntity(entityId2, Category.Entity);
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
        assertTrue(entityMgr.hasEntity(entityId, Category.Entity));

        assertEquals(entity, entityMgr.unregisterEntity(entityId, Category.Entity).orElse(null));
        assertFalse(entityMgr.hasEntity(entityId, Category.Entity));

        assertEquals(Optional.empty(), entityMgr.unregisterEntity(entityId, Category.Entity));
    }

    @Test
    void testGetEntityMap() {

        final String entityId1 = UUID.randomUUID().toString();
        final String entityId2 = UUID.randomUUID().toString();
        final Entity entity1 = new Entity(entityId1);
        final Entity entity2 = new Entity(entityId2);
        assertTrue(entityMgr.registerEntity(entity1));
        assertTrue(entityMgr.registerEntity(entity2));

        final Map<String, IEntity> entityMap = entityMgr.getEntityMap(Category.Entity);

        assertEquals(2, entityMap.size());
        assertThrows(UnsupportedOperationException.class, () -> entityMap.remove(entityId1));

        entityMgr.unregisterEntity(entityId1, Category.Entity);
        assertEquals(1, entityMap.size());

        entityMgr.unregisterEntity(entityId2, Category.Entity);
        assertEquals(0, entityMap.size());
    }

    @Test
    @DisplayName("测试EventRegisterEntityPre/EventRegisterEntityPost/EventUnregisterEntityPre/EventUnregisterEntityPost是否可以按照顺序触发")
    void testEventRegister() {
        final IGameLoopEventBus iGameLoopEventBus = new GameLoopEventBus(gameLoop);
        gameLoop.addComponent(IGameLoopEventBus.class, iGameLoopEventBus);

        final String entityId = UUID.randomUUID().toString();
        final Entity entity = new Entity(entityId);
        final Component4EventRegister component = new Component4EventRegister(entity, 0);
        entity.addComponent(Component4EventRegister.class, component);

        entityMgr.registerEntity(entity);
        entityMgr.unregisterEntity(entityId, Category.Entity);

        final Integer actual = entity.getComponent(Component4EventRegister.class)
                .map(Component4EventRegister::getCounter)
                .orElse(0);

        assertEquals(4, actual);

        gameLoop.removeComponent(IGameLoopEventBus.class);
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

    @SuppressWarnings("unused")
    public static class Component4EventRegister extends Component<IEntity> {
        @Getter
        private int counter;

        public Component4EventRegister(IEntity owner, int counter) {
            super(owner);
            this.counter = counter;
        }

        @Subscribe
        private void eventRegisterEntityPre(final EventRegisterEntityPre event) {

            if (getId().equals(event.getEntityId()) && counter == 0) {
                counter++;
            }
        }

        @Subscribe
        private void eventRegisterEntityPost(final EventRegisterEntityPost event) {

            if (getId().equals(event.getEntityId()) && counter == 1) {
                counter++;
            }
        }

        @Subscribe
        private void EventUnregisterEntityPre(final EventUnregisterEntityPre event) {

            if (getId().equals(event.getEntityId()) && counter == 2) {
                counter++;
            }
        }

        @Subscribe
        private void EventUnregisterEntityPost(final EventUnregisterEntityPost event) {

            if (getId().equals(event.getEntityId()) && counter == 3) {
                counter++;
            }
        }

    }
}