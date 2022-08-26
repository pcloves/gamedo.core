package org.gamedo.gameloop.components.eventbus.interfaces;

import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IIdentity;
import org.gamedo.gameloop.components.eventbus.EventData;

/**
 * 这是一个专门针对{@link IIdentity}类型订阅者进行过滤检测的事件，当且仅当订阅者满足如下时，事件才会派发给订阅者：
 * <ul>
 * <li> 订阅者的类型是{@link IIdentity}或其子类（例如：{@link IComponent}或{@link IEntity}）
 * <li> 订阅者的Id：{@link IIdentity#getId()}等于{@link IIdentitySelfEvent#getId()}
 * </ul>
 * 该事件的应用场景非常广泛，例如对于一个游戏玩家实体，当达成一项成就或角色等级升级时，需要通知外界该行为的发生，而绝大部分情况下，仅仅该玩家自身及
 * 其组件关心该行为，此时就可以向事件总线投递该事件，以玩家角色升级这种场景为例，首先需要定义等级变化事件：
 * <pre>
 * public class EventPlayerLevelChangePost implements IIdentitySelfEvent
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
 *     public String getId() {
 *         return entityId;
 *     }
 * }
 * </pre>
 * 当玩家升级时，向所在的线程投递该事件：
 * <pre>
 * final String entityId = ...
 * final int levelOld = ...
 * final int levelNew = ...
 * //定义等级事件
 * final Supplier&lt;EventPlayerLevelChangePost&gt; event = () -&gt; new EventPlayerLevelChangePost(entityId, levelOld, levelNew);
 * //向所在线程投递升级事件
 * GameLoops.current().map(gameLoop -&gt; gameLoop.submit(IGameLoopEventBusFunction.post(EventPlayerLevelChangePost.class, event)));
 * </pre>
 * 实际上对于玩家等级变化的行为，仍然也可以使用非过滤功能的原生{@link IEvent}向外界投递消息，但是带来的代价就是：
 * <ul>
 * <li> 所有EventPlayerLevelChangePost的事件订阅者都会触发该事件，这会带来额外性能消耗
 * <li> 订阅者不得不在事件处理函数里增加额外的逻辑，以确定是否要处理该事件
 * </ul>
 */
@SuppressWarnings("unused")
public interface IIdentitySelfEvent extends IIdentityEvent {

    String getId();

    @Override
    default boolean filter(EventData eventData, IIdentityEvent event) {
        return IIdentityEvent.super.filter(eventData, event) && ((IIdentity) eventData.getObject()).getId().equals(getId());
    }
}
