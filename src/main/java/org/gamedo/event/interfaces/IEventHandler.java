package org.gamedo.event.interfaces;

import java.io.Serializable;
import java.util.function.Consumer;

@FunctionalInterface
public interface IEventHandler<E extends IEvent> extends Consumer<E>, Serializable
{
    @Override
    void accept(E e);
}
