package org.gamedo.gameloop.components.entitymanager;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.event.EventRegisterEntityPost;
import org.gamedo.event.EventRegisterEntityPre;
import org.gamedo.event.EventUnregisterEntityPost;
import org.gamedo.event.EventUnregisterEntityPre;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.Markers;
import org.gamedo.util.GamedoConfiguration;
import org.gamedo.util.Metric;
import org.gamedo.util.Pair;
import org.gamedo.util.function.GameLoopFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.gamedo.util.function.IGameLoopSchedulerFunction;
import org.gamedo.util.function.IGameLoopTickManagerFunction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Log4j2
public class GameLoopEntityManager extends GameLoopComponent implements IGameLoopEntityManager {
    private final Map<String, IEntity> entityMap = new HashMap<>(512);
    private final Map<String, Pair<AtomicLong, Gauge>> entityClazzMap = new HashMap<>(4);

    public GameLoopEntityManager(IGameLoop owner) {
        super(owner);
    }

    @Override
    public <T extends IEntity> boolean registerEntity(T entity) {

        final String entityId = entity.getId();
        if (entityMap.containsKey(entityId)) {
            log.error(Markers.GameLoopEntityManager, "the entity has registered, entityId:{}", entityId);
            return false;
        }

        log.debug(Markers.GameLoopEntityManager, "register begin, entityId:{}", () -> entityId);

        final Class<? extends IEntity> entityClazz = entity.getClass();
        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopEntityManager, "the {} hasn't a owner yet.", GameLoopEntityManager.class.getSimpleName());
            return false;
        }

        //1 ??????Pre??????
        final EventRegisterEntityPre eventRegisterEntityPre = new EventRegisterEntityPre(entityId, owner);
        final GameLoopFunction<Integer> eventPreFunction = IGameLoopEventBusFunction.post(eventRegisterEntityPre);
        owner.submit(eventPreFunction);

        //2 ??????IEntity???????????????
        owner.submit(IGameLoopEventBusFunction.register(entity));
        //2.1 ???????????????????????????
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopEventBusFunction.register(component)));

        //3 ??????IEntity???@Cron??????
        owner.submit(IGameLoopSchedulerFunction.register(entity));
        //3.1 ???????????????@Cron??????
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopSchedulerFunction.register(component)));

        //4 ??????IEntity???@Tick??????
        owner.submit(IGameLoopTickManagerFunction.register(entity));
        //4.1 ???????????????@Tick??????
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopTickManagerFunction.register(component)));

        //5 ????????????
        entityMap.put(entityId, entity);

        //6 ??????Post??????
        final EventRegisterEntityPost registerEntityPost = new EventRegisterEntityPost(entityId, owner);
        final GameLoopFunction<Integer> eventPostFunction = IGameLoopEventBusFunction.post(registerEntityPost);
        owner.submit(eventPostFunction);

        log.debug(Markers.GameLoopEntityManager, "register finish, entityId:{}", () -> entityId);

        metricGauge(entityClazz);

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IEntity> Optional<T> unregisterEntity(String entityId) {

        final IEntity entity = entityMap.get(entityId);
        if (entity == null) {
            return Optional.empty();
        }

        final Class<? extends IEntity> entityClazz = entity.getClass();
        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopEntityManager, "the {} hasn't a owner yet.", GameLoopEntityManager.class.getSimpleName());
            return Optional.empty();
        }

        //1 ??????Pre??????
        final EventUnregisterEntityPre eventUnregisterEntityPre = new EventUnregisterEntityPre(entityId, owner);
        final GameLoopFunction<Integer> eventPreFunction = IGameLoopEventBusFunction.post(eventUnregisterEntityPre);
        owner.submit(eventPreFunction);

        //2 ??????????????????@Tick??????
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopTickManagerFunction.unregister(component)));
        //2.1 ?????????IEntity???@Tick??????
        owner.submit(IGameLoopTickManagerFunction.unregister(entity));

        //3 ??????????????????@Cron??????
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopSchedulerFunction.unregister(component.getClass())));
        //3.1 ?????????IEntity???@Cron??????
        owner.submit(IGameLoopSchedulerFunction.unregister(entity.getClass()));

        //4 ??????????????????????????????
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopEventBusFunction.unregister(component)));
        //4.1 ?????????IEntity???????????????
        owner.submit(IGameLoopEventBusFunction.unregister(entity));

        //5 ?????????????????????
        entityMap.remove(entityId);

        //6 ??????post??????
        final EventUnregisterEntityPost eventUnregisterEntityPost = new EventUnregisterEntityPost(entityId, owner);
        final GameLoopFunction<Integer> eventPostFunction = IGameLoopEventBusFunction.post(eventUnregisterEntityPost);
        owner.submit(eventPostFunction);

        metricGauge(entityClazz);

        return Optional.of((T)entity);
    }

    @Override
    public boolean hasEntity(String entityId) {
        return entityMap.containsKey(entityId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IEntity> Optional<T> getEntity(String entityId) {
        return Optional.ofNullable((T)entityMap.get(entityId));
    }

    @Override
    public int getEntityCount() {
        return entityMap.size();
    }

    @Override
    public Map<String, IEntity> getEntityMap() {
        return Collections.unmodifiableMap(entityMap);
    }

    private void metricGauge(Class<? extends IEntity> entityClazz) {
        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopEntityManager, "the {} hasn't a owner yet.", GameLoopEntityManager.class.getSimpleName());
            return;
        }

        owner.getComponent(MeterRegistry.class)
                .map(meterRegistry -> GamedoConfiguration.isMetricEntityEnable() ? meterRegistry : null)
                .ifPresent(meterRegistry -> {

                    final Tags tags = Metric.tags(owner);
                    final long entityCountNew = getEntityCount(entityClazz);

                    entityClazzMap.computeIfAbsent(entityClazz.getSimpleName(), key -> {
                                final AtomicLong count = new AtomicLong(entityCountNew);
                                return Pair.of(count,
                                        Gauge.builder(Metric.MeterIdEntityGauge, count, AtomicLong::longValue)
                                                .tags(tags.and("type", key))
                                                .description("the total IEntity count of a specific type")
                                                .baseUnit(BaseUnits.OBJECTS)
                                                .register(meterRegistry));
                            }
                    ).getK().set(entityCountNew);
                });
    }

    private long getEntityCount(Class<?> clazz) {
        return entityMap.values()
                .stream()
                .filter(entity -> entity.getClass().equals(clazz))
                .count();
    }
}
