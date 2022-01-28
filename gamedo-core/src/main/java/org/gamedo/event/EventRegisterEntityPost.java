package org.gamedo.event;

import lombok.Value;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.interfaces.IGameLoop;

@Value
public class EventRegisterEntityPost implements IEvent {
    String entityId;
    IGameLoop gameLoop;
}
