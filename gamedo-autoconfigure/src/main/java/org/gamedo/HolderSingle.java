package org.gamedo;

import org.gamedo.gameloop.interfaces.IGameLoopGroup;

/**
 * 实现延迟加载，当且仅当{@link Gamedo#single() Gamedo.single}被调用时，{@link HolderSingle#single}线程组才会被初始化，并且由
 * jvm的class lock确保线程安全
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
final class HolderSingle {
    static final IGameLoopGroup single = Gamedo.context().getBean(IGameLoopGroup.class, Gamedo.gameLoopProperties.getSingles().convert());

    private HolderSingle() {
    }
}
