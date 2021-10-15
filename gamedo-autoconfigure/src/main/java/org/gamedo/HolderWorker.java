package org.gamedo;

import org.gamedo.gameloop.interfaces.IGameLoopGroup;

/**
 * 实现延迟加载，当且仅当{@link Gamedo#worker() Gamedo.worker()}被调用时，{@link HolderWorker#worker}线程组才会被初始化，并且由
 * jvm的class lock确保线程安全
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
final class HolderWorker {
    static final IGameLoopGroup worker = Gamedo.context().getBean(IGameLoopGroup.class, Gamedo.gameLoopProperties.getWorkers().convert());

    private HolderWorker() {
    }
}
