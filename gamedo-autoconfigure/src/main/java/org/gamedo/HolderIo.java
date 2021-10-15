package org.gamedo;

import org.gamedo.gameloop.interfaces.IGameLoopGroup;

/**
 * 实现延迟加载，当且仅当{@link Gamedo#io() Gamedo.io()}被调用时，{@link HolderIo#io}线程组才会被初始化，并且由jvm的class lock
 * 确保线程安全
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
final class HolderIo {
    static final IGameLoopGroup io = Gamedo.context().getBean(IGameLoopGroup.class, Gamedo.gameLoopProperties.getIos().convert());

    private HolderIo() {
    }
}
