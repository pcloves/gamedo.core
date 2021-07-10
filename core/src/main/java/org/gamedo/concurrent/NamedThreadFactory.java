package org.gamedo.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private static final AtomicInteger ID = new AtomicInteger(1);
    private final String name;

    public NamedThreadFactory(final String namePrefix) {
        name = namePrefix + '-' + ID.getAndIncrement();
    }

    public Thread newThread(Runnable r) {
        return new Thread(r, name);
    }
}
