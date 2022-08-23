package org.gamedo.gameloop.components.eventbus.interfaces;

import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.Subscribe;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.EntityComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.GameLoop;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;

import java.util.Arrays;
import java.util.Comparator;
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
        gameLoop.addComponent(MyGenericComponent.class, new MyGenericComponent(gameLoop));
    }

    @Test
    void testRegister() {
        final Optional<MyComponent> componentOptional = gameLoop.getComponent(MyComponent.class);
        final MyComponent myComponent = Assertions.assertDoesNotThrow(componentOptional::get);

        final int registerMethodCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(2, registerMethodCount);

        final int registerMethodCount1 = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(0, registerMethodCount1);
    }

    @Test
    void testRegisterInSubClass() {
        final Optional<MySubComponent> componentOptional = gameLoop.getComponent(MySubComponent.class);
        final MySubComponent mySubComponent = Assertions.assertDoesNotThrow(componentOptional::get);

        final int registerMethodCount = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(3, registerMethodCount);

        final int registerMethodCount1 = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(0, registerMethodCount1);
    }

    @Test
    void testUnregister() {

        final Optional<MyComponent> componentOptional = gameLoop.getComponent(MyComponent.class);
        final MyComponent myComponent = Assertions.assertDoesNotThrow(componentOptional::get);

        final int unregisterCount = iGameLoopEventBus.unregister(myComponent);
        Assertions.assertEquals(0, unregisterCount);

        final int registerCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(2, registerCount);

        final int unregisterCount1 = iGameLoopEventBus.unregister(myComponent);
        Assertions.assertEquals(2, unregisterCount1);
    }

    @Test
    void testPostInSubClass() {
        final Optional<MySubComponent> componentOptional = gameLoop.getComponent(MySubComponent.class);
        final MySubComponent mySubComponent = Assertions.assertDoesNotThrow(componentOptional::get);

        final int registerMethodCount = iGameLoopEventBus.register(mySubComponent);
        Assertions.assertEquals(3, registerMethodCount);

        final int postValue = ThreadLocalRandom.current().nextInt();
        iGameLoopEventBus.post(EventTest.class, () -> new EventTest(postValue));

        Assertions.assertEquals(postValue, mySubComponent.getValue());
    }

    @Test
    void testPost() {
        final Optional<MyComponent> componentOptional = gameLoop.getComponent(MyComponent.class);
        final MyComponent myComponent = Assertions.assertDoesNotThrow(componentOptional::get);

        final int registerMethodCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(2, registerMethodCount);

        final int postValue = ThreadLocalRandom.current().nextInt();
        iGameLoopEventBus.post(EventTest.class, () -> new EventTest(postValue));

        Assertions.assertEquals(postValue, myComponent.getValue());
    }

    @Test
    void testGenericEventPost() {
        final Optional<MyGenericComponent> componentOptional = gameLoop.getComponent(MyGenericComponent.class);
        final MyGenericComponent myComponent = Assertions.assertDoesNotThrow(componentOptional::get);

        final int registerMethodCount = iGameLoopEventBus.register(myComponent);
        Assertions.assertEquals(2, registerMethodCount);

        iGameLoopEventBus.post(MyGenericEvent.class, () -> new MyGenericEvent<>(Integer.class));
        iGameLoopEventBus.post(MyGenericEvent.class, () -> new MyGenericEvent<>(Integer.class));
        Assertions.assertEquals(2, myComponent.getIntValue());
        Assertions.assertEquals(0, myComponent.getStringValue());

        iGameLoopEventBus.post(MyGenericEvent.class, () -> new MyGenericEvent<>(String.class));
        Assertions.assertEquals(2, myComponent.getIntValue());
        Assertions.assertEquals(1, myComponent.getStringValue());
    }

    @Test
    void testCircularPost() {
        final CircularComponent component = new CircularComponent(gameLoop, iGameLoopEventBus);

        iGameLoopEventBus.register(component);
        iGameLoopEventBus.post(EventTest.class, () -> new EventTest(1));
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

        iGameLoopEventBus.post(EventTest.class, () -> new EventTest(value));
        //所有组件都会监听到该事件
        Assertions.assertEquals(value, component1.value);
        Assertions.assertEquals(value, component2.value);
        Assertions.assertEquals(Integer.MAX_VALUE, myObject.myIdentityEventValue);

        iGameLoopEventBus.post(MyIdentityEvent.class, () -> new MyIdentityEvent(entityId1, value));
        //只有一个组件都会监听到该事件
        Assertions.assertEquals(value, component1.myIdentityEventValue);
        Assertions.assertEquals(Integer.MAX_VALUE, component2.myIdentityEventValue);
        Assertions.assertEquals(Integer.MAX_VALUE, myObject.myIdentityEventValue);
    }

    @Test
    void testPriorityEvent() {
        final PriorityObject priorityObject = new PriorityObject();

        //很可惜，通过反射获取到的函数并非按照定义的顺序排序，因此这里需要按照函数名排序，以满足测试用例
        Arrays.stream(ReflectionUtils.getAllDeclaredMethods(PriorityObject.class))
                .map(method -> Pair.of(method, method.getAnnotation(Subscribe.class)))
                .filter(pair -> pair.getV() != null)
                .sorted(Comparator.comparing(pair -> pair.getK().getName()))
                .forEach(pair -> iGameLoopEventBus.register(priorityObject, pair.getK(), pair.getV().value()));

        iGameLoopEventBus.post(EventTest.class, () -> new EventTest(0));

        Assertions.assertEquals('n', priorityObject.index);
    }

    @SuppressWarnings("unused")
    private static class PriorityObject {
        private char index = '0';

        @Subscribe(Short.MIN_VALUE)
        private void a(final EventTest eventTest) {

            if (index == '0') {
                index = 'a';
            }
        }

        @Subscribe(Short.MIN_VALUE)
        private void b(final EventTest eventTest) {

            if (index == 'a') {
                index = 'b';
            }
        }

        @Subscribe(Short.MIN_VALUE + 1)
        private void c(final EventTest eventTest) {

            if (index == 'b') {
                index = 'c';
            }
        }

        @Subscribe(Short.MIN_VALUE + 1)
        private void d(final EventTest eventTest) {

            if (index == 'c') {
                index = 'd';
            }
        }


        @Subscribe(-1)
        private void e(final EventTest eventTest) {

            if (index == 'd') {
                index = 'e';
            }
        }

        @Subscribe(-1)
        private void f(final EventTest eventTest) {

            if (index == 'e') {
                index = 'f';
            }
        }

        @Subscribe
        private void g(final EventTest eventTest) {

            if (index == 'f') {
                index = 'g';
            }
        }

        @Subscribe
        private void h(final EventTest eventTest) {

            if (index == 'g') {
                index = 'h';
            }
        }

        @Subscribe(1)
        private void i(final EventTest eventTest) {

            if (index == 'h') {
                index = 'i';
            }
        }

        @Subscribe(1)
        private void j(final EventTest eventTest) {

            if (index == 'i') {
                index = 'j';
            }
        }

        @Subscribe(Short.MAX_VALUE - 1)
        private void k(final EventTest eventTest) {

            if (index == 'j') {
                index = 'k';
            }
        }

        @Subscribe(Short.MAX_VALUE - 1)
        private void l(final EventTest eventTest) {

            if (index == 'k') {
                index = 'l';
            }
        }

        @Subscribe(Short.MAX_VALUE)
        private void m(final EventTest eventTest) {

            if (index == 'l') {
                index = 'm';
            }
        }

        @Subscribe(Short.MAX_VALUE)
        private void n(final EventTest eventTest) {

            if (index == 'm') {
                index = 'n';
            }
        }
    }

    public static class MyGenericEvent<R> implements IGenericEvent<R> {
        private final Class<R> clazz;

        public MyGenericEvent(Class<R> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<R> getGenericType() {
            return clazz;
        }
    }

    @SuppressWarnings("unused")
    @Getter
    private static class MyGenericComponent extends EntityComponent {
        protected int stringValue;
        protected int intValue;

        public MyGenericComponent(IEntity owner) {
            super(owner);
        }

        @Subscribe
        private void myGenericEventString(final MyGenericEvent<String> myEntityEvent) {
            stringValue++;
        }

        @Subscribe
        private void myGenericEventInt(final MyGenericEvent<Integer> myEntityEvent) {
            intValue++;
        }
    }


    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static class EventPlayerLevelUpPost implements IIdentitySelfEvent {
        private final String entityId;
        private final int levelOld;
        private final int levelNew;

        private EventPlayerLevelUpPost(String entityId, int levelOld, int levelNew) {
            this.entityId = entityId;
            this.levelOld = levelOld;
            this.levelNew = levelNew;
        }

        @Override
        public String getId() {
            return entityId;
        }
    }

    private static class MyIdentityEvent implements IIdentitySelfEvent {
        private final String targetEntityId;
        private final int value;

        private MyIdentityEvent(String targetEntityId, int value) {
            this.targetEntityId = targetEntityId;
            this.value = value;
        }

        @Override
        public String getId() {
            return targetEntityId;
        }
    }

    @SuppressWarnings("unused")
    @Data
    private static class MyObject {
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
            eventBus.post(EventTest.class, () -> new EventTest(count++));
        }
    }
}