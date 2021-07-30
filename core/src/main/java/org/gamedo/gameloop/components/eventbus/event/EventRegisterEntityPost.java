package org.gamedo.gameloop.components.eventbus.event;

import lombok.Value;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;

@Value
public class EventRegisterEntityPost implements IEvent {
    String entityId;
    String gameLoopId;
}
