package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.GameLoopFunction;

import java.util.Optional;

public interface IEntityFunction {

    /**
     * 定义一个行为：向某个实体中增加一个组件
     * @param clazz 组件的类
     * @param component 要增加的组件
     * @param <T> 组件的类型
     * @return 返回行为本身
     */
    static <T> GameLoopFunction<Optional<T>> addComponent(Class<T> clazz, T component) {
        return gameLoop -> gameLoop.addComponent(clazz, component);
    }
}
