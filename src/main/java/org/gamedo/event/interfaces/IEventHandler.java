package org.gamedo.event.interfaces;

import java.util.function.Function;

@FunctionalInterface
public interface IEventHandler<E extends IEvent, R> extends Function<E, R> {

}
