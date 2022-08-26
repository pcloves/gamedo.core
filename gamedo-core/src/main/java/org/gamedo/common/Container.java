package org.gamedo.common;

import lombok.extern.log4j.Log4j2;
import org.gamedo.logging.Markers;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused"})
@Log4j2
public class Container<T extends IContainerKey> implements IContainer<T> {
    private final Map<String, Object> dataMap;

    public Container(int initialCapacity) {
        this.dataMap = new HashMap<>(initialCapacity);
    }

    @Override
    public IContainer<T> put(T key, Object data) {

        @SuppressWarnings("DuplicatedCode") final Class<?> typeExpect = key.getType();
        final Class<?> typeActual = data.getClass();
        if (!typeExpect.isAssignableFrom(typeActual)) {
            log.error(Markers.GameLoopContainer, "invalid data type:{}, expected:{}", typeActual.getSimpleName(), typeExpect.getSimpleName());
            return null;
        }

        this.dataMap.put(key.get(), data);

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V get(T key) {
        final Object data = dataMap.get(key.get());
        if (data == null) {
            return null;
        }

        final Class<?> typeExpect = key.getType();
        final Class<?> typeActual = data.getClass();
        if (!typeExpect.isAssignableFrom(typeActual)) {
            log.error(Markers.GameLoopContainer, "invalid data type:{}, expected:{}", typeActual.getSimpleName(), typeExpect.getSimpleName());
            return null;
        }

        return (V) data;
    }

}
