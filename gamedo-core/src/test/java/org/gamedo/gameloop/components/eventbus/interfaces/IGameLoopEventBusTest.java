package org.gamedo.gameloop.components.eventbus.interfaces;

import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.Subscribe;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.EntityComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IIdentity;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Log4j2
class IGameLoopEventBusTest {

    private final IGameLoop gameLoop = Mockito.spy(new GameLoop("IGameLoopEventBusTest"));
    private final IGameLoopEventBus iGameLoopEventBus = new GameLoopEventBus(gameLoop);

    IGameLoopEventBusTest() {
        Mockito.when(gameLoop.inThread()).thenReturn(true);

        gameLoop.addComponent(MyComponent.class, new MyComponent(gameLoop));
        gameLoop.addComponent(MySubComponent.class, new MySubComponent(gameLoop));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testRegister() {
        final Optional<MyComponent> componentOptional = gameLoop.getComponent(MyComponent.class);
        final MyComponent myComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int registerMethodCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(1, registerMethodCount);

        final int registerMethodCount1 = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(0, registerMethodCount1);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testRegisterInSubClass() {
        final Optional<MySubComponent> componentOptional = gameLoop.getComponent(MySubComponent.class);
        final MySubComponent mySubComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int registerMethodCount = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(2, registerMethodCount);

        final int registerMethodCount1 = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(0, registerMethodCount1);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testUnregister() {

        final Optional<MyComponent> componentOptional = gameLoop.getComponent(MyComponent.class);
        final MyComponent myComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int unregisterCount = iGameLoopEventBus.unregister(myComponent);
        Assertions.assertEquals(0, unregisterCount);

        final int registerCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(1, registerCount);

        final int unregisterCount1 = iGameLoopEventBus.unregister(myComponent);
        Assertions.assertEquals(1, unregisterCount1);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testPostInSubClass() {
        final Optional<MySubComponent> componentOptional = gameLoop.getComponent(MySubComponent.class);
        final MySubComponent mySubComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int registerMethodCount = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(2, registerMethodCount);

        final int postValue = ThreadLocalRandom.current().nextInt();
        iGameLoopEventBus.post(new EventTest(postValue));

        Assertions.assertEquals(postValue, mySubComponent.getValue());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testPost() {
        final Optional<MyComponent> componentOptional = gameLoop.getComponent(MyComponent.class);
        final MyComponent myComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int registerMethodCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(1, registerMethodCount);

        final int postValue = ThreadLocalRandom.current().nextInt();
        iGameLoopEventBus.post(new EventTest(postValue));

        Assertions.assertEquals(postValue, myComponent.getValue());
    }

    @Test
    void testCircularPost() {
        final CircularComponent component = new CircularComponent(gameLoop, iGameLoopEventBus);

        iGameLoopEventBus.register(component);
        iGameLoopEventBus.post(new EventTest(1));
    }

    @Test
    void testEntityEventPost() {
        final String entityId1 = UUID.randomUUID().toString();
        final String entityId2 = UUID.randomUUID().toString();

        final int value = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        final MyComponent component1 = new MyComponent(new Entity(entityId1), Integer.MAX_VALUE, Integer.MAX_VALUE);
        final MyComponent component2 = new MyComponent(new Entity(entityId2), Integer.MAX_VALUE, Integer.MAX_VALUE);
        final MyObject myObject = new MyObject(Integer.MAX_VALUE);

        iGameLoopEventBus.register(component1);
        iGameLoopEventBus.register(component2);
        iGameLoopEventBus.register(myObject);

        iGameLoopEventBus.post(new EventTest(value));
        //所有组件都会监听到该事件
        Assertions.assertEquals(value, component1.value);
        Assertions.assertEquals(value, component2.value);
        Assertions.assertEquals(Integer.MAX_VALUE, myObject.myIdentityEventValue);

        iGameLoopEventBus.post(new MyIdentityEvent(entityId1, value));
        //只有一个组件都会监听到该事件
        Assertions.assertEquals(value, component1.myIdentityEventValue);
        Assertions.assertEquals(Integer.MAX_VALUE, component2.myIdentityEventValue);
        Assertions.assertEquals(Integer.MAX_VALUE, myObject.myIdentityEventValue);
    }

    private static class EventPlayerLevelUpPost implements IIdentityEvent
    {
        private final String entityId;
        private final int levelOld;
        private final int levelNew;

        private EventPlayerLevelUpPost(String entityId, int levelOld, int levelNew) {
            this.entityId = entityId;
            this.levelOld = levelOld;
            this.levelNew = levelNew;
        }

        @Override
        public boolean filter(IIdentity subscriber) {
            return entityId.equals(subscriber.getId());
        }
    }

    private static class MyIdentityEvent implements IIdentityEvent
    {
        private final String targetEntityId;
        private final int value;

        private MyIdentityEvent(String targetEntityId, int value) {
            this.targetEntityId = targetEntityId;
            this.value = value;
        }

        @Override
        public boolean filter(IIdentity subscriber) {
            return targetEntityId.equals(subscriber.getId());
        }
    }

    @Data
    private static class MyObject
    {
        private int myIdentityEventValue;

        private MyObject(int myIdentityEventValue) {
            this.myIdentityEventValue = myIdentityEventValue;
        }

        @SuppressWarnings("unused")
        @Subscribe
        private void myEntityEvent(final MyIdentityEvent myEntityEvent) {
            myIdentityEventValue = myEntityEvent.value;
        }

        public void setMyIdentityEventValue(int myIdentityEventValue) {
            this.myIdentityEventValue = myIdentityEventValue;
        }
    }

    @Value
    private static class EventTest implements IEvent {
        int value;
    }

    @Getter
    private static class MyComponent extends EntityComponent {
        protected int value;
        private int myIdentityEventValue;

        private MyComponent(IEntity owner) {
            super(owner);
        }

        private MyComponent(IEntity owner, int value, int myIdentityEventValue) {
            super(owner);
            this.value = value;
            this.myIdentityEventValue = myIdentityEventValue;
        }

        @SuppressWarnings("unused")
        @Subscribe
        private void eventTest(final EventTest eventTest) {
            value = eventTest.value;
        }

        @SuppressWarnings("unused")
        @Subscribe
        private void myIdentityEvent(final MyIdentityEvent myEntityEvent) {
            myIdentityEventValue = myEntityEvent.value;
        }
    }

    private static class MySubComponent extends MyComponent {
        private MySubComponent(IEntity owner) {
            super(owner);
        }

        @SuppressWarnings("unused")
        @Subscribe
        private void eventTestSub(final EventTest eventTest) {
            //do nothing
        }
    }

    private static class CircularComponent extends EntityComponent {

        private final IGameLoopEventBus eventBus;
        private int count = 1;

        private CircularComponent(IEntity owner, IGameLoopEventBus eventBus) {
            super(owner);
            this.eventBus = eventBus;
        }

        @SuppressWarnings("unused")
        @Subscribe
        private void eventTest(final EventTest eventTest) {
            log.info("eventTest {}", eventTest);
            eventBus.post(new EventTest(count++));
        }
    }
}