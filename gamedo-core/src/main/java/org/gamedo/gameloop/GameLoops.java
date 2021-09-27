package org.gamedo.gameloop;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Optional;

public final class GameLoops {
    public static final ThreadLocal<Optional<IGameLoop>> GAME_LOOP_THREAD_LOCAL = ThreadLocal.withInitial(Optional::empty);

    private GameLoops() {
    }

    /**
     * 返回当前的{@link IGameLoop}，当且仅当调用方法在某个{@link IGameLoop}线程的调用栈内时，返回所属的{@link IGameLoop}，否则返回
     * {@link Optional#empty()}
     *
     * @return 当前所归属的IGameLoop
     */
    public static Optional<IGameLoop> current() {
        return GAME_LOOP_THREAD_LOCAL.get();
    }
}
