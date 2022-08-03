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
import org.gamedo.logging.GamedoLogContext;
import org.gamedo.logging.Markers;
import org.gamedo.util.GamedoConfiguration;
import org.gamedo.util.Metric;
import org.gamedo.util.Pair;
import org.gamedo.util.function.GameLoopFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.gamedo.util.function.IGameLoopSchedulerFunction;
import org.gamedo.util.function.IGameLoopTickManagerFunction;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

@Log4j2
public class GameLoopEntityManager extends GameLoopComponent implements IGameLoopEntityManager {
    public static final Function<String, Map<String, IEntity>> entityMapFunction = category -> new HashMap<>(128);
    private final Map<String, Map<String, IEntity>> entityCategoryMap = new HashMap<>(4);
    private final Map<String, Pair<AtomicLong, Gauge>> entityClazzGaugeMap = new HashMap<>(4);

    public GameLoopEntityManager(IGameLoop owner) {
        super(owner);
    }

    @Override
    public <T extends IEntity> boolean registerEntity(T entity) {

        final String entityId = entity.getId();
        try(final GamedoLogContext.CloseableEntityId ignored = GamedoLogContext.pushEntityIdAuto(entityId)) {
            final String category = entity.getCategory();
            final Map<String, IEntity> entityMap = entityCategoryMap.computeIfAbsent(category, entityMapFunction);
            if (entityMap.containsKey(entityId)) {
                log.error(Markers.GameLoopEntityManager, "the entity has registered, entityId:{}", entityId);
                return false;
            }

            log.debug(Markers.GameLoopEntityManager, "register begin, entityId:{}", () -> entityId);

            final IGameLoop owner = ownerRef.get();
            if (owner == null) {
                log.error(Markers.GameLoopEntityManager, "the {} hasn't a owner yet.", GameLoopEntityManager.class.getSimpleName());
                return false;
            }

            //1 首先注册IEntity的事件监听
            owner.submit(IGameLoopEventBusFunction.register(entity));
            //1.1 注册组件的事件监听
            entity.getComponentMap().values()
                    .stream()
                    .distinct()
                    .forEach(component -> owner.submit(IGameLoopEventBusFunction.register(component)));

            //2 触发Pre事件
            final Supplier<EventRegisterEntityPre> eventRegisterEntityPre = () -> new EventRegisterEntityPre(entityId, category, owner);
            final GameLoopFunction<Integer> eventPreFunction = IGameLoopEventBusFunction.post(EventRegisterEntityPre.class, eventRegisterEntityPre);
            owner.submit(eventPreFunction);

            //3 注册IEntity的@Cron方法
            owner.submit(IGameLoopSchedulerFunction.register(entity));
            //3.1 注册组件的@Cron方法
            entity.getComponentMap().values()
                    .stream()
                    .distinct()
                    .forEach(component -> owner.submit(IGameLoopSchedulerFunction.register(component)));

            //4 注册IEntity的@Tick方法
            owner.submit(IGameLoopTickManagerFunction.register(entity));
            //4.1 注册组件的@Tick方法
            entity.getComponentMap().values()
                    .stream()
                    .distinct()
                    .forEach(component -> owner.submit(IGameLoopTickManagerFunction.register(component)));

            //5 加入管理
            entityMap.put(entityId, entity);

            //6 触发Post事件
            final Supplier<EventRegisterEntityPost> eventRegisterEntityPost = () -> new EventRegisterEntityPost(entityId, category, owner);
            final GameLoopFunction<Integer> eventPostFunction = IGameLoopEventBusFunction.post(EventRegisterEntityPost.class, eventRegisterEntityPost);
            owner.submit(eventPostFunction);

            log.debug(Markers.GameLoopEntityManager, "register finish, entityId:{}", () -> entityId);

            metricGauge(() -> category);

            return true;
        } catch (Exception e) {
            log.error(Markers.GameLoopEntityManager, "exception caught, entity id:" + entityId, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IEntity> Optional<T> unregisterEntity(String entityId, Supplier<String> category) {

        final Map<String, IEntity> entityMap = entityCategoryMap.computeIfAbsent(category.get(), entityMapFunction);
        final IEntity entity = entityMap.get(entityId);
        if (entity == null) {
            return Optional.empty();
        }

        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopEntityManager, "the {} hasn't a owner yet.", GameLoopEntityManager.class.getSimpleName());
            return Optional.empty();
        }

        //1 触发Pre事件
        final Supplier<EventUnregisterEntityPre> eventUnregisterEntityPre = () -> new EventUnregisterEntityPre(entityId, category.get(), owner);
        final GameLoopFunction<Integer> eventPreFunction = IGameLoopEventBusFunction.post(EventUnregisterEntityPre.class, eventUnregisterEntityPre);
        owner.submit(eventPreFunction);

        //2 反注册组件的@Tick函数
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopTickManagerFunction.unregister(component)));
        //2.1 反注册IEntity的@Tick函数
        owner.submit(IGameLoopTickManagerFunction.unregister(entity));

        //3 反注册组件的@Cron函数
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopSchedulerFunction.unregister(component.getClass())));
        //3.1 反注册IEntity的@Cron函数
        owner.submit(IGameLoopSchedulerFunction.unregister(entity.getClass()));

        //4 然后移除管理
        entityMap.remove(entityId);

        //5 再触发post事件
        final Supplier<EventUnregisterEntityPost> eventUnregisterEntityPost = () -> new EventUnregisterEntityPost(entityId, category.get(), owner);
        final GameLoopFunction<Integer> eventPostFunction = IGameLoopEventBusFunction.post(EventUnregisterEntityPost.class, eventUnregisterEntityPost);
        owner.submit(eventPostFunction);

        //6 最后反注册组件的事件监听
        entity.getComponentMap().values()
                .stream()
                .distinct()
                .forEach(component -> owner.submit(IGameLoopEventBusFunction.unregister(component)));
        //6.1 反注册IEntity的事件监听
        owner.submit(IGameLoopEventBusFunction.unregister(entity));

        metricGauge(category);

        return Optional.of((T) entity);
    }

    public boolean hasEntity(String entityId, Supplier<String> category) {
        return entityCategoryMap.computeIfAbsent(category.get(), entityMapFunction).containsKey(entityId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IEntity> Optional<T> getEntity(String entityId, Supplier<String> category) {
        return Optional.ofNullable((T) entityCategoryMap.computeIfAbsent(category.get(), entityMapFunction).get(entityId));
    }

    @Override
    public int getEntityCount(Supplier<String> category) {
        return entityCategoryMap.computeIfAbsent(category.get(), entityMapFunction).size();
    }

    @Override
    public Map<String, IEntity> getEntityMap(Supplier<String> category) {
        return Collections.unmodifiableMap(entityCategoryMap.computeIfAbsent(category.get(), entityMapFunction));
    }

    private void metricGauge(Supplier<String> entityClazz) {
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

                    entityClazzGaugeMap.computeIfAbsent(entityClazz.get(), key -> {
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
}
