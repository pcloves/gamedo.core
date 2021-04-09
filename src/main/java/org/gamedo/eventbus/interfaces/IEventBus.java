package org.gamedo.eventbus.interfaces;

import org.gamedo.ecs.interfaces.IComponent;

public interface IEventBus extends IComponent {

    /**
     * 注册所有的包含{@link Subscribe}注解的方法
     *
     * @param object 要进行注册的实例
     * @return 返回注册成功的方法数量
     */
    int register(Object object);

    /**
     * 取消注册所有包含{@link Subscribe}注解的方法
     *
     * @param object 找进行取消注册的实例
     * @return 返回取消注册成功的方法数量
     */
    int unregister(Object object);

    /**
     * 投递一个事件到{@link IEventBus}上
     *
     * @param iEvent 要投递的事件
     * @return 正常消费该事件的数量（抛出异常的事件处理器不包含在内）
     */
    int post(IEvent iEvent);
}
