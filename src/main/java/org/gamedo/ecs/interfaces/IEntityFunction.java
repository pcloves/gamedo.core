package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Optional;

public interface IEntityFunction {
    /**
     * 定义一个行为：向{@link IGameLoop}添加一个事件
     *
     * @return 返回该行为的定义，其中GameLoopFunction中的Optional<T>代表实体的旧的组件
     */
    static <T> GameLoopFunction<Optional<T>> addComponent(Class<T> clazz, T component) {
        return gameLoop -> gameLoop.addComponent(clazz, component);
    }
}
