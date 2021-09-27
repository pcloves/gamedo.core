package org.gamedo.gameloop.components.eventbus.event;

import lombok.Value;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.interfaces.IGameLoop;

@Value
public class EventGameLoopCreatePost implements IEvent {
    IGameLoop gameLoop;
}