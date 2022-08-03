package org.gamedo.gameloop;

import java.util.function.Supplier;

public enum Category implements Supplier<String> {
    Entity,
    ;

    @Override
    public String get() {
        return name();
    }
}
