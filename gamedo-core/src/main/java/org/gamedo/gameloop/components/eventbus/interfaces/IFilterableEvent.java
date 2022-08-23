package org.gamedo.gameloop.components.eventbus.interfaces;

import org.gamedo.gameloop.components.eventbus.EventData;

/**
 * 这是一个过滤特性的事件，只有当订阅者通过了事件检测（{@link IFilterableEvent#filter(EventData, IFilterableEvent)} ）后才往该订阅者派发事件
 */
public interface IFilterableEvent<T extends IFilterableEvent<T>> extends IEvent {
    /**
     * 对订阅者进行过滤检测，只有返回true时，本事件才会派发给subscriber
     *
     * @param eventData 要被检测的订阅者信息
     * @param event     要过滤的事件
     * @return true表示该订阅者关心该事件，false表示事件订阅者不关心该事件
     */
    boolean filter(EventData eventData, T event);
}
