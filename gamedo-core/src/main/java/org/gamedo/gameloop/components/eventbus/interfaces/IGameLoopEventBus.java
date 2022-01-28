package org.gamedo.gameloop.components.eventbus.interfaces;

import org.gamedo.annotation.Subscribe;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.lang.reflect.Method;

public interface IGameLoopEventBus extends IComponent<IGameLoop> {

    /**
     * 注册所有的包含{@link Subscribe}注解的方法
     *
     * @param object 要进行注册的实例
     * @return 返回注册成功的方法数量
     */
    int register(Object object);

    /**
     * 手动注册一个包含{@link Subscribe}注解的方法
     * @param object 要进行注册的实例
     * @param method 要注册的方法，该方法只能包含一个参数，且参数类型为{@link IEvent}子类
     * @param priority 注册优先级
     * @return 注册成功返回true
     */
    boolean register(Object object, Method method, short priority);

    /**
     * 取消注册所有包含{@link Subscribe}注解的方法
     *
     * @param object 找进行取消注册的实例
     * @return 返回取消注册成功的方法数量
     */
    int unregister(Object object);

    /**
     * 投递一个事件到{@link IGameLoopEventBus}上
     *
     * @param iEvent 要投递的事件
     * @return 正常消费该事件的数量（抛出异常的事件处理器不包含在内）
     */
    int post(IEvent iEvent);
}
