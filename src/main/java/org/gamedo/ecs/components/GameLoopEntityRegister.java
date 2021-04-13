package org.gamedo.ecs.components;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IGameLoopEntityRegister;
import org.gamedo.eventbus.event.EventPreRegisterEntity;
import org.gamedo.eventbus.event.EventPreUnregisterEntity;
import org.gamedo.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Log4j2
public class GameLoopEntityRegister extends Component implements IGameLoopEntityRegister {
    private final IGameLoop gameLoop;
    private final Map<String, IEntity> entityMap = new HashMap<>(512);

    public GameLoopEntityRegister(IGameLoop gameLoop, IEntity owner, Map<String, IEntity> entityMap) {
        super(owner);
        this.gameLoop = gameLoop;
        this.entityMap.putAll(entityMap != null ? entityMap : Collections.emptyMap());
    }

    @Override
    public boolean registerEntity(IEntity entity) {
        if (entityMap.containsKey(entity.getId())) {
            return false;
        }

        //先设置所归属的IGameLoop，使其可以执行一些操作，例如注册事件
        entity.setBelongedGameLoop(gameLoop);

        //再触发事件
        final Optional<IGameLoopEventBus> eventBus = owner.getComponent(IGameLoopEventBus.class);
        eventBus.ifPresent(iEventBus -> iEventBus.post(new EventPreRegisterEntity(entity.getId())));

        //最后加入管理
        entityMap.put(entity.getId(), entity);

        return true;
    }

    @Override
    public Optional<IEntity> unregisterEntity(String entityId) {
        final IEntity iEntity = entityMap.get(entityId);
        if (iEntity == null) {
            return Optional.empty();
        }

        final Optional<IGameLoopEventBus> eventBus = owner.getComponent(IGameLoopEventBus.class);
        //先触发事件
        eventBus.ifPresent(iEventBus -> iEventBus.post(new EventPreUnregisterEntity(iEntity.getId())));

        //再清空所归属的IGameLoop
        iEntity.setBelongedGameLoop(null);

        //最后移除管理
        entityMap.remove(entityId);

        return Optional.of(iEntity);
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
