package org.gamedo.common.interfaces;

@FunctionalInterface
public interface Priority {
    Priority Hightest = () -> Short.MAX_VALUE;
    Priority Higher14 = () -> (short) (1 << 14);
    Priority Higher13 = () -> (short) (1 << 13);
    Priority Higher12 = () -> (short) (1 << 12);
    Priority Higher11 = () -> (short) (1 << 11);
    Priority Higher10 = () -> (short) (1 << 10);
    Priority Higher9 = () -> (short) (1 << 9);
    Priority Higher8 = () -> (short) (1 << 8);
    Priority Higher7 = () -> (short) (1 << 7);
    Priority Higher6 = () -> (short) (1 << 6);
    Priority Higher5 = () -> (short) (1 << 5);
    Priority Higher4 = () -> (short) (1 << 4);
    Priority Higher3 = () -> (short) (1 << 3);
    Priority Higher2 = () -> (short) (1 << 2);
    Priority Higher1 = () -> (short) (1 << 1);
    Priority Normal = () -> (short) 0;
    Priority Lower1 = () -> (short) (-1 << 1);
    Priority Lower2 = () -> (short) (-1 << 2);
    Priority Lower3 = () -> (short) (-1 << 3);
    Priority Lower4 = () -> (short) (-1 << 4);
    Priority Lower5 = () -> (short) (-1 << 5);
    Priority Lower6 = () -> (short) (-1 << 6);
    Priority Lower7 = () -> (short) (-1 << 7);
    Priority Lower8 = () -> (short) (-1 << 8);
    Priority Lower9 = () -> (short) (-1 << 9);
    Priority Lower10 = () -> (short) (-1 << 10);
    Priority Lower11 = () -> (short) (-1 << 11);
    Priority Lower12 = () -> (short) (-1 << 12);
    Priority Lower13 = () -> (short) (-1 << 13);
    Priority Lower14 = () -> (short) (-1 << 14);
    Priority Lowest = () -> Short.MIN_VALUE;

    short getPriority();
}
