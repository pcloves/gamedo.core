package org.gamedo.util.function;

import org.gamedo.ecs.interfaces.IEntity;

import java.util.function.Function;

@FunctionalInterface
public interface EntityFunction<T extends IEntity, R> extends Function<T, R> {
    @Override
    R apply(T t);
}
