package org.gamedo.event.imp;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.impl.Entity;
import org.gamedo.event.interfaces.EventPriority;
import org.gamedo.event.interfaces.IEvent;
import org.gamedo.event.interfaces.IEventBus;
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

    private int myEventValue;
    private IEventBus eventBus;

    private Boolean onMyEvent(MyEvent event) {
        Assertions.assertEquals(myEventValue, event.value);

        return true;
    }

    @BeforeEach
    void setUp() {
        eventBus = new EventBus(new Entity("test"));
        myEventValue = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void registerEvent() {

        eventBus.registerEvent(MyEvent.class, this::onMyEvent, EventPriority.Normal);
        eventBus.sendEvent(new MyEvent(myEventValue));
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