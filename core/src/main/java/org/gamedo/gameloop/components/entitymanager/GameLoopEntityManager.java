package org.gamedo.gameloop.components.entitymanager;

import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.GamedoComponent;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.event.EventRegisterEntityPost;
import org.gamedo.gameloop.components.eventbus.event.EventRegisterEntityPre;
import org.gamedo.gameloop.components.eventbus.event.EventUnregisterEntityPost;
import org.gamedo.gameloop.components.eventbus.event.EventUnregisterEntityPre;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.gamedo.util.function.IGameLoopSchedulerFunction;
import org.gamedo.util.function.IGameLoopTickManagerFunction;
import org.gamedo.util.function.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.Markers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Log4j2
@GamedoComponent
public class GameLoopEntityManager extends GameLoopComponent implements IGameLoopEntityManager {
    private final Map<String, IEntity> entityMap = new HashMap<>(512);

    public GameLoopEntityManager(IGameLoop owner) {
        super(owner);
    }

    @Override
    public boolean registerEntity(IEntity entity) {

        final String entityId = entity.getId();
        if (entityMap.containsKey(entityId)) {
            log.error(Markers.GameLoopEntityManager, "the entity has registered, entityId:{}", entityId);
            return false;
        }

        log.debug(Markers.GameLoopEntityManager, "register begin, entityId:{}", () -> entityId);

        //1 先将IEntity注册到IGameLoopEventBus，使之可以响应事件
        owner.submit(IGameLoopEventBusFunction.register(entity));
        //1.1 再注册所有的组件
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopEventBusFunction.register(component)));

        //2
        owner.submit(IGameLoopSchedulerFunction.register(entity));
        //2.1
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopSchedulerFunction.register(component)));

        //3
        owner.submit(IGameLoopTickManagerFunction.register(entity));
        //3.1
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopTickManagerFunction.register(component)));

        final String id = owner.getId();
        //4 然后触发事件
        final GameLoopFunction<Integer> eventPre = IGameLoopEventBusFunction.post(new EventRegisterEntityPre(entityId, id));
        owner.owner().ifPresentOrElse(iGameLoopGroup -> {
            iGameLoopGroup.submitAll(eventPre);
        }, () -> owner.submit(eventPre));

        //5 再然后加入管理
        entityMap.put(entityId, entity);

        //6 至此已经完全加入管理，再通知一次
        final GameLoopFunction<Integer> eventPost = IGameLoopEventBusFunction.post(new EventRegisterEntityPost(entityId, id));
        owner.owner().ifPresentOrElse(iGameLoopGroup -> {
            iGameLoopGroup.submitAll(eventPost);
        }, () -> owner.submit(eventPost));

        log.debug(Markers.GameLoopEntityManager, "register finish, entityId:{}", () -> entityId);
        return true;
    }

    @Override
    public Optional<IEntity> unregisterEntity(String entityId) {

        final IEntity entity = entityMap.get(entityId);
        if (entity == null) {
            return Optional.empty();
        }

        //1. 先触发事件
        final String id = owner.getId();
        final GameLoopFunction<Integer> eventPre = IGameLoopEventBusFunction.post(new EventUnregisterEntityPre(entityId, id));
        owner.owner().ifPresentOrElse(iGameLoopGroup -> iGameLoopGroup.submitAll(eventPre),
                () -> owner.submit(eventPre));

        //2 先移除组件的组成
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopTickManagerFunction.unregister(component)));
        //2.1 再移除自身的注册
        owner.submit(IGameLoopTickManagerFunction.unregister(entity));

        //3
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopSchedulerFunction.unregister(component.getClass())));
        //3.1
        owner.submit(IGameLoopSchedulerFunction.unregister(entity.getClass()));

        //4
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopEventBusFunction.unregister(component)));
        //4.1
        owner.submit(IGameLoopEventBusFunction.unregister(entity));

        //5 再然后移除管理
        entityMap.remove(entityId);

        //6 最后一次事件通知
        final GameLoopFunction<Integer> eventPost = IGameLoopEventBusFunction.post(new EventUnregisterEntityPost(entityId, id));
        owner.owner().ifPresentOrElse(iGameLoopGroup -> iGameLoopGroup.submitAll(eventPost),
                () -> owner.submit(eventPost));

        return Optional.of(entity);
    }

    @Override
    public boolean hasEntity(String entityId) {
        return entityMap.containsKey(entityId);
    }

    @Override
    public int getEntityCount() {
        return entityMap.size();
    }

    @Override
    public Map<String, IEntity> getEntityMap() {
        return Collections.unmodifiableMap(entityMap);
    }
}
