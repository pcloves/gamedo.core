package org.gamedo.concurrent;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {

    private final String name;
    private final boolean daemon;

    public NamedThreadFactory(final String name, boolean daemon) {
        this.name = name;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread thread = new Thread(r, name);
        thread.setDaemon(daemon);

        return thread;
    }
}
