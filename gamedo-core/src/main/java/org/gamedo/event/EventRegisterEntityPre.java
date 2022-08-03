package org.gamedo.event;

import lombok.Value;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.interfaces.IGameLoop;

/**
 * 实体注册到某{@link IGameLoop}前触发事件，此时还不能在{@link IGameLoop}中取到该实体
 */
@Value
public class EventRegisterEntityPre implements IEvent {
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
