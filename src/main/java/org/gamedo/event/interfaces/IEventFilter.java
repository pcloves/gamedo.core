package org.gamedo.event.interfaces;

import java.util.function.Predicate;

@FunctionalInterface
public interface IEventFilter<IEvent> extends Predicate<IEvent> {

}
