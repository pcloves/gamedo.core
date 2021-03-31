package org.gamedo.event.imp;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.Entity;
import org.gamedo.event.EventBus;
import org.gamedo.event.interfaces.EventPriority;
import org.gamedo.event.interfaces.IEvent;
import org.gamedo.event.interfaces.IEventBus;
import org.gamedo.event.interfaces.IEventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
@SpringBootTest
@Slf4j
class EventBusTest {
    private static int HandleCount;
    @SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
    private static final IEventHandler<MyEvent> MY_EVENTHANDLER = myEvent -> HandleCount++;

    private final IEventHandler<MyEvent> myEventIEventHandler = this::onMyEvent;
    private int myEventValue;
    private IEventBus eventBus;
    private int handleCount;

    private Boolean onMyEvent(MyEvent event) {
        Assertions.assertEquals(myEventValue, event.value);

        handleCount++;

        return true;
    }

    @BeforeEach
    void setUp() {

        eventBus = new EventBus(new Entity("test"));
        myEventValue = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        HandleCount = 0;
        handleCount = 0;
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void registerEventUsingLambda() {

        final boolean registerResult = eventBus.registerEvent(MyEvent.class, myEvent -> handleCount++, EventPriority.Normal);
        Assertions.assertTrue(registerResult);

        final boolean registerResult1 = eventBus.registerEvent(MyEvent.class, myEvent -> handleCount++, EventPriority.Normal);
        Assertions.assertTrue(registerResult1);

        eventBus.sendEvent(new MyEvent(myEventValue));

        Assertions.assertEquals(2, handleCount);
    }

    @Test
    void registerEventUsingMethodReference() {

        final boolean registerResult = eventBus.registerEvent(MyEvent.class, this::onMyEvent, EventPriority.Normal);
        Assertions.assertTrue(registerResult);

        final boolean registerResult1 = eventBus.registerEvent(MyEvent.class, this::onMyEvent, EventPriority.Normal);
        Assertions.assertTrue(registerResult1);

        eventBus.sendEvent(new MyEvent(myEventValue));

        Assertions.assertEquals(2, handleCount);
    }

    @Test
    void registerEventUsingField() {

        final boolean registerResult = eventBus.registerEvent(MyEvent.class, myEventIEventHandler, EventPriority.Normal);
        Assertions.assertTrue(registerResult);

        final boolean registerResult1 = eventBus.registerEvent(MyEvent.class, myEventIEventHandler, EventPriority.Normal);
        Assertions.assertFalse(registerResult1);

        eventBus.sendEvent(new MyEvent(myEventValue));

        Assertions.assertEquals(1, handleCount);
    }

    @Test
    void registerEventUsingStaticField() {

        final boolean registerResult = eventBus.registerEvent(MyEvent.class, MY_EVENTHANDLER, EventPriority.Normal);
        Assertions.assertTrue(registerResult);

        final boolean registerResult1 = eventBus.registerEvent(MyEvent.class, MY_EVENTHANDLER, EventPriority.Normal);
        Assertions.assertFalse(registerResult1);

        eventBus.sendEvent(new MyEvent(myEventValue));

        Assertions.assertEquals(1, HandleCount);
    }


    @Test
    void unRegisterEvent() {
    }

    @Test
    void addEventFilter() {
    }

    @Test
    void removeEventFilter() {
    }

    @Test
    void sendEvent() {
    }

    @Test
    void postEvent() {
    }

    @Test
    void dispatchCachedEvent() {
    }

    @Value
    private static class MyEvent implements IEvent {
        int value;
    }
}