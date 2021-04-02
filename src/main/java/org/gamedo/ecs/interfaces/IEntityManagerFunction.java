package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

public interface IEntityManagerFunction {
    /**
     * 定义一个行为：将某个{@link IEntity}注册到一个{@link IGameLoop}上
     *
     * @param entity 要注册的实体
     * @return 返回该行为的定义，其中GameLoopFunction中的Boolean代表注册是否成功
     */
    static GameLoopFunction<Boolean> registerEntity(final IEntity entity) {
        return gameLoop -> gameLoop.getComponent(IEntityManager.class)
                .map(iEntityMgr -> iEntityMgr.registerEntity(entity))
                .orElse(false);
    }

    /**
     * 定义一个行为：检测某个{@link IEntity}是否已经注册到某{@link IGameLoop}上
     *
     * @param entityId 实体的id
     * @return 返回该行为的定义，其中GameLoopFunction中的Boolean代表是否注册了该实体
     */
    static GameLoopFunction<Boolean> hasEntity(final String entityId) {
        return gameLoop -> gameLoop.getComponent(IEntityManager.class)
                .map(iEntityManager -> iEntityManager.hasEntity(entityId))
                .orElse(false);
    }

    /**
     * 定义一个行为：检测某个{@link IEntityManager}管理的实体的数量
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表实体的数量
     */
    static GameLoopFunction<Integer> getEntityCount() {
        return gameLoop -> gameLoop.getComponent(IEntityManager.class)
                .map(iEntityManager -> iEntityManager.getEntityCount())
                .orElse(0);
    }
}
