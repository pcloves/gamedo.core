package org.gamedo.eventbus.event;

import lombok.Value;
import org.gamedo.eventbus.interfaces.IEvent;

@Value
public class EventPreUnregisterEntity implements IEvent {
    String entityId;
}
