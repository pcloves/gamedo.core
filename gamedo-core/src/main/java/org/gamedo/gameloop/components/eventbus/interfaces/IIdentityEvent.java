package org.gamedo.gameloop.components.eventbus.interfaces;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IIdentity;

/**
 * 这是一个专门针对{@link IIdentity}类型订阅者进行过滤检测的{@link IFilterableEvent}事件，根据父接口{@link IFilterableEvent}的约定：
 * 订阅者首先必须是：{@link IIdentity}类型，才会继续第2条的检测，在gamedo框架默认实现情况下，有两种接口继承自{@link IIdentity}，分别为：
 * <ul>
 * <li>{@link IEntity}
 * <li>{@link IComponent}
 * </ul>
 * 该事件的应用场景非常广泛，例如对于一个游戏玩家实体，当达成一项成就或角色等级升级时，需要通知外界该行为的发生，而绝大部分情况下，仅仅该玩家自身及
 * 其组件关心该行为，此时就可以向事件总线投递该事件，以玩家角色升级这种场景为例，首先需要定义等级变化事件：
 * <pre>
 * private static class EventPlayerLevelChangePost implements IIdentityEvent
 * {
 *     private final String entityId;
 *     private final int levelOld;
 *     private final int levelNew;
 *
 *     private EventPlayerLevelChangePost(String entityId, int levelOld, int levelNew) {
 *         this.entityId = entityId;
 *         this.levelOld = levelOld;
 *         this.levelNew = levelNew;
 *     }
 *
 *     &#64;Override
 *     public boolean filter(IIdentity subscriber) {
 *         //只有订阅者的id和本事件id相同，这个订阅者才会收到本事件
 *         return entityId.equals(subscriber.getId());
 *     }
 * }
 * </pre>
 * 当玩家升级时，向所在的线程投递该事件：
 * <pre>
 * final String entityId = ...
 * final int levelOld = ...
 * final int levelNew = ...
 * //定义等级事件
 * final EventPlayerLevelChangePost event = newEventPlayerLevelChangePost (entityId, 1, 2);
 * //向所在线程投递升级事件
 * GameLoops.current().map(gameLoop -&#62;  gameLoop.submit(IGameLoopEventBusFunction.post(event)));
 * </pre>
 * 实际上，对于玩家等级变化的行为，仍然也可以使用非过滤功能的原生{@link IEvent}向外界投递消息，但是带来的代价就是：
 * <ul>
 * <li> 所有的事件订阅者都会触发该事件，这会带来额外性能消耗
 * <li> 订阅者不得不在事件处理函数里增加额外的逻辑，以确定是否要处理该事件
 * </ul>
 */
@FunctionalInterface
public interface IIdentityEvent extends IFilterableEvent<IIdentity> {

    @Override
    default Class<IIdentity> getType() {
        return IIdentity.class;
    }

    @Override
    boolean filter(IIdentity subscriber);
}
