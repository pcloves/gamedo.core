package org.gamedo.gameloop.components.eventbus.interfaces;

import lombok.Getter;
import lombok.Value;
import org.gamedo.annotation.Subscribe;
import org.gamedo.ecs.EntityComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

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
        iGameLoopEventBus.post(new MyEvent(postValue));

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

        @SuppressWarnings("unused")
        @Subscribe
        private void myEvent(final MyEvent myEvent) {
            value = myEvent.value;
        }
    }

    private static class MySubComponent extends MyComponent {
        private MySubComponent(IEntity owner) {
            super(owner);
        }

        @SuppressWarnings("unused")
        @Subscribe
        private void myEvent(final MyEvent myEvent) {
            //do nothing
        }
    }
}