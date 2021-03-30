package org.gamedo.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private static AtomicInteger id = new AtomicInteger(1);
    private final String name;

    public NamedThreadFactory(final String namePrefix) {
        name = namePrefix + '-' + id.getAndIncrement();
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name);
    }
}
