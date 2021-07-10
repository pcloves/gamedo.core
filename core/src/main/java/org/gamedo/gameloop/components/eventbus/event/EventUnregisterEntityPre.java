package org.gamedo.gameloop.components.eventbus.event;

import lombok.Value;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;

@Value
public class EventUnregisterEntityPre implements IEvent {
    String entityId;
}
