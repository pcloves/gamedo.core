package org.gamedo.gameloop.components.eventbus;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.lang.reflect.Method;

@Value
@EqualsAndHashCode
public class EventData {
    Object object;
    Method method;
}
