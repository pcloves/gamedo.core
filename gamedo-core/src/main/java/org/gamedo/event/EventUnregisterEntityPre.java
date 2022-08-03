package org.gamedo.event;

import lombok.Value;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.interfaces.IGameLoop;

/**
 * 实体从某{@link IGameLoop}反注册前触发事件，此时还能在{@link IGameLoop}中取到该实体
 */
@Value
public class EventUnregisterEntityPre implements IEvent {
    /**
     * 实体id
     */
    String entityId;
    /**
     * 实体所属分类
     */
    String category;
    /**
     * 注册到哪个{@link IGameLoop}上
     */
    IGameLoop gameLoop;
}
