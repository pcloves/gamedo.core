package org.gamedo.application;

import lombok.Value;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * {@link GamedoApplication}对该类型的{@link Bean}会执行如下两个操作：
 * <ul>
 * <li>调用{@link IEntity#addComponent(Class, Object)}，将其注册为应用程序级的全局组件
 * <li>如果发现类型是{@link IGameLoopGroup}则调用{@link IGameLoopGroup#run(long, long, TimeUnit)}开启循环
 * </ul>
 *
 * @param <T> 组件要暴露给外界的接口类型
 * @param <R> 组件的实际实现类型
 */
@Value
public class GamedoApplicationComponentRegister<T, R extends T> {
    Class<T> clazz;
    R object;
}
