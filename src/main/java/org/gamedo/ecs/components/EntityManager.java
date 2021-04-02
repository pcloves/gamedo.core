package org.gamedo.ecs.components;

import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IEntityManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class EntityManager extends Component implements IEntityManager
{
    private final Map<String, IEntity> entityMap = new HashMap<>(512);

    public EntityManager(IEntity owner, Map<String, IEntity> entityMap) {
        super(owner);
        this.entityMap.putAll(entityMap != null ? entityMap : Collections.emptyMap());
    }

    @Override
    public boolean registerEntity(IEntity entity)
    {
        if (entityMap.containsKey(entity.getId())) {
            return false;
        }

        entityMap.put(entity.getId(), entity);
        return true;
    }

    @Override
    public Optional<IEntity> unregisterEntity(String entityId) {
        return Optional.ofNullable(entityMap.remove(entityId));
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
