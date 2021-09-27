package org.gamedo.ecs.interfaces;

import java.util.Optional;

public interface IInterfaceQueryable {

    <T> Optional<T> getInterface(Class<T> clazz);
}
