package org.gamedo.gameloop.components.eventbus.interfaces;

import org.gamedo.gameloop.components.eventbus.EventData;

/**
 * 泛型事件，解决由于Java对泛型的类型擦除，导致无法使用泛型事件的问题，使用方法参考相关测试用例
 * @see <a href=https://blog.csdn.net/AdobeSolo/article/details/73698631>EventBus框架总结之支持泛型参数</a>
 */
public interface IGenericEvent<R> extends IFilterableEvent<IGenericEvent<R>> {

    Class<R> getGenericType();

    @Override
    default boolean filter(EventData eventData, IGenericEvent<R> event) {
        final Class<R> genericType = event.getGenericType();
        final Class<?> genericClazz = eventData.getGenericClazz();

        return genericType == genericClazz;
    }
}
