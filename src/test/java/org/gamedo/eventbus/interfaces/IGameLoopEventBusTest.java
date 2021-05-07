package org.gamedo.eventbus.interfaces;

import lombok.Getter;
import lombok.Value;
import org.gamedo.ecs.EntityComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

class IGameLoopEventBusTest {

    private final IGameLoop entity = new GameLoop(UUID.randomUUID().toString());
    private final IGameLoopEventBus iGameLoopEventBus = new GameLoopEventBus(entity);

    IGameLoopEventBusTest() {
        entity.addComponent(MyComponent.class, new MyComponent(entity));
        entity.addComponent(MySubComponent.class, new MySubComponent(entity));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testRegister() {
        final Optional<MyComponent> componentOptional = entity.getComponent(MyComponent.class);
        final MyComponent myComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int registerMethodCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(1, registerMethodCount);

        final int registerMethodCount1 = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(0, registerMethodCount1);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testRegisterInSubClass() {
        final Optional<MySubComponent> componentOptional = entity.getComponent(MySubComponent.class);
        final MySubComponent mySubComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int registerMethodCount = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(2, registerMethodCount);

        final int registerMethodCount1 = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(0, registerMethodCount1);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testUnregister() {

        final Optional<MyComponent> componentOptional = entity.getComponent(MyComponent.class);
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
        final Optional<MySubComponent> componentOptional = entity.getComponent(MySubComponent.class);
        final MySubComponent mySubComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int registerMethodCount = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(2, registerMethodCount);

        final int postValue = ThreadLocalRandom.current().nextInt();
        iGameLoopEventBus.post(new MyEvent(postValue));

        Assertions.assertEquals(postValue, mySubComponent.getValue());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testPost() {
        final Optional<MyComponent> componentOptional = entity.getComponent(MyComponent.class);
        final MyComponent myComponent = Assertions.assertDoesNotThrow(() -> componentOptional.get());

        final int registerMethodCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(1, registerMethodCount);

        final int postValue = ThreadLocalRandom.current().nextInt();
        iGameLoopEventBus.post(new MyEvent(postValue));

        Assertions.assertEquals(postValue, myComponent.getValue());
    }

    @Value
    private static class MyEvent implements IEvent {
        int value;
    }

    private static class MyComponent extends EntityComponent {
        @Getter
        protected int value;

        private MyComponent(IEntity owner) {
            super(owner);
        }

        @Subscribe
        private void myEvent(final MyEvent myEvent) {
            value = myEvent.value;
        }
    }

    private static class MySubComponent extends MyComponent {
        private MySubComponent(IEntity owner) {
            super(owner);
        }

        @Subscribe
        private void myEvent(final MyEvent myEvent) {
            //do nothing
        }
    }
}