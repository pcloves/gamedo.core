package org.gamedo.gameloop.components.eventbus;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.Subscribe;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.GamedoLogContext;
import org.gamedo.logging.Markers;
import org.gamedo.util.GamedoConfiguration;
import org.gamedo.util.Metric;
import org.gamedo.util.Pair;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class GameLoopEventBus extends GameLoopComponent implements IGameLoopEventBus {
    private final Map<Class<? extends IEvent>, List<EventData>> eventClazzName2EventDataMap = new HashMap<>(128);
    private final Deque<Class<?>> eventPostStack = new LinkedList<>();
    private final Map<String, Pair<AtomicLong, Gauge>> eventClazzName2GaugeMap = new HashMap<>(128);

    public GameLoopEventBus(IGameLoop owner) {
        super(owner);
    }

    private boolean safeInvoke(EventData eventData, IEvent event) {
        final Object object = eventData.getObject();
        final Method method = eventData.getMethod();

        final Timer timer = owner.getComponent(MeterRegistry.class)
                .map(meterRegistry -> GamedoConfiguration.isMetricEventEnable() ? meterRegistry : null)
                .map(meterRegistry -> {

                    final Tags tags = Metric.tags(owner);
                    return Timer.builder(Metric.MeterIdEventTimer)
                            .tags(tags)
                            .tag("class", object.getClass().getName())
                            .tag("method", method.getName())
                            .tag("event", event.getClass().getSimpleName())
                            .description("the @" + Subscribe.class.getSimpleName() + " method timing.")
                            .register(meterRegistry);
                })
                .orElse(Metric.NOOP_TIMER);

        return timer.record(() -> {
            try (final GamedoLogContext.CloseableEntityId ignored = GamedoLogContext.pushEntityIdAuto(object)) {
                ReflectionUtils.makeAccessible(method);
                method.invoke(object, event);
                return true;
            } catch (Exception e) {
                final Class<? extends IEvent> eventClazz = event.getClass();
                log.error(Markers.GameLoopEventBus, "exception caught, method:" + method.getName() +
                        ", event:" + eventClazz.getName(), e);
            }

            return false;
        });
    }

    @Override
    public int register(Object object) {

        final Class<?> clazz = object.getClass();
        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.info(Markers.GameLoopEventBus, "none annotation {} method found, clazz:{}",
                    Subscribe.class.getSimpleName(),
                    clazz.getName());
            return 0;
        }

        final int count = (int) annotatedMethodSet.stream()
                .filter(method -> method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic())
                .filter(method -> register(object, method))
                .count();

        log.debug(Markers.GameLoopEventBus, "register eventBus finish, clazz:{}, totalCount:{}, successCount:{}",
                clazz.getSimpleName(),
                annotatedMethodSet.size(),
                count
        );

        return count;
    }

    private boolean register(Object object, Method method) {
        if (!method.isAnnotationPresent(Subscribe.class)) {
            log.error(Markers.GameLoopEventBus, "the method {} of class {} is not annotated by '{}'",
                    method.getName(),
                    object.getClass().getName(),
                    Subscribe.class.getSimpleName());
            return false;
        }

        if (method.getParameterCount() != 1) {
            log.error(Markers.GameLoopEventBus, "the method {} of class {} is required one parameter.",
                    method.getName(),
                    object.getClass().getName());
            return false;
        }

        final Class<?> eventClazz = method.getParameterTypes()[0];
        if (!IEvent.class.isAssignableFrom(eventClazz)) {
            log.error(Markers.GameLoopEventBus, "the parameter type of method {} of class {} is not " +
                            "assignable from {}",
                    method.getName(),
                    object.getClass().getName(),
                    IEvent.class.getName());
            return false;
        }

        //noinspection unchecked
        return register(object, method, (Class<? extends IEvent>) eventClazz);
    }

    private <T extends IEvent> boolean register(Object object, Method method, Class<T> eventClazz) {
        final Function<Class<? extends IEvent>, List<EventData>> function = eventClazz1 -> new ArrayList<>(32);
        final List<EventData> eventDataList = eventClazzName2EventDataMap.computeIfAbsent(eventClazz, function);

        final EventData eventData = new EventData(object, method);
        if (eventDataList.contains(eventData)) {
            log.warn(Markers.GameLoopEventBus, "the event has registered, event clazz:{}, object clazz:{}, " +
                            "method:{}",
                    eventClazz.getSimpleName(),
                    object.getClass().getSimpleName(),
                    method.getName());
            return false;
        }

        final boolean add = eventDataList.add(eventData);

        final List<EventData> duplicateEventDataList = eventDataList.stream()
                .filter(eventData1 -> eventData1.getObject() == object)
                .collect(Collectors.toList());

        if (duplicateEventDataList.size() > 1) {
            final List<Method> list = duplicateEventDataList.stream()
                    .map(eventData1 -> eventData1.getMethod())
                    .collect(Collectors.toList());
            log.warn(Markers.GameLoopEventBus, "multiply methods register on the same event:{}, object:{}, " +
                    "method list:{}", eventClazz, object.getClass(), list);
        }

        log.debug(Markers.GameLoopEventBus, "register, event clazz:{}, object clazz:{}, method:{}, result:{}",
                () -> eventClazz.getSimpleName(),
                () -> object.getClass().getSimpleName(),
                () -> method.getName(),
                () -> add
        );


        metricGauge(eventClazz, eventDataList);

        return add;
    }

    @Override
    public int unregister(Object object) {

        final Class<?> objectClazz = object.getClass();
        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(objectClazz))
                .filter(method -> method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        return (int) annotatedMethodSet.stream()
                .filter(method -> method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic())
                .filter(method -> unregister(object, method))
                .count();
    }

    private boolean unregister(Object object, Method method) {

        if (!method.isAnnotationPresent(Subscribe.class)) {
            log.error(Markers.GameLoopEventBus, "the method {} is not annotated by '{}'",
                    method.getName(), Subscribe.class.getSimpleName());
            return false;
        }

        if (method.getParameterCount() != 1) {
            log.error(Markers.GameLoopEventBus, "the method {} is required one parameter.", method.getName());
        }

        final Class<?> eventClazz = method.getParameterTypes()[0];
        if (!IEvent.class.isAssignableFrom(eventClazz)) {
            log.error(Markers.GameLoopEventBus, "the parameter type of method {} is not assignable from {}",
                    method.getName(),
                    IEvent.class.getName());
            return false;
        }

        //noinspection unchecked
        return unregister(object, method, (Class<? extends IEvent>) eventClazz);
    }

    private <T extends IEvent> boolean unregister(Object object, Method method, Class<T> eventClazz) {

        final Function<Class<? extends IEvent>, List<EventData>> function = eventClazz1 -> new ArrayList<>(32);
        final List<EventData> eventDataList = eventClazzName2EventDataMap.computeIfAbsent(eventClazz, function);

        final EventData eventData = new EventData(object, method);
        final boolean remove = eventDataList.remove(eventData);

        log.debug(Markers.GameLoopEventBus, "unregister, event clazz:{}, object clazz:{}, method:{}, result:{}",
                () -> eventClazz.getSimpleName(),
                () -> object.getClass().getSimpleName(),
                () -> method.getName(),
                () -> remove);

        metricGauge(eventClazz, eventDataList);

        return remove;
    }

    private <T extends IEvent> void metricGauge(Class<T> eventClazz, List<EventData> eventDataList) {
        owner.getComponent(MeterRegistry.class)
                .map(meterRegistry -> GamedoConfiguration.isMetricEventEnable() ? meterRegistry : null)
                .ifPresent(meterRegistry -> {
                    final long countNew = eventDataList.size();
                    eventClazzName2GaugeMap.computeIfAbsent(eventClazz.getSimpleName(), key -> {
                        final Tags tags = Metric.tags(owner);
                        final Tag tag = Tag.of("event", key);
                        final AtomicLong count = new AtomicLong(countNew);

                        return Pair.of(count, Gauge.builder(Metric.MeterIdEventRegisterGauge, count, AtomicLong::longValue)
                                .tags(tags.and(tag))
                                .baseUnit(BaseUnits.OBJECTS)
                                .description("the instance count of a specific @" + Subscribe.class.getSimpleName())
                                .register(meterRegistry)
                        );
                    }).getK().set(countNew);
                });
    }

    @Override
    public int post(IEvent iEvent) {

        final Class<? extends IEvent> eventClazz = iEvent.getClass();
        final Optional<List<EventData>> optionalEventDataList = Optional.ofNullable(eventClazzName2EventDataMap.get(eventClazz));
        if (optionalEventDataList.isEmpty()) {
            return 0;
        }

        if (eventPostStack.size() > GamedoConfiguration.getMaxEventPostDepth()) {
            final List<String> eventClazzList = eventPostStack.stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.toList());
            log.error(Markers.GameLoopEventBus,
                    "post event overflow, max depth:{}, current stack:{}",
                    GamedoConfiguration.getMaxEventPostDepth(),
                    eventClazzList);
            return 0;
        }

        eventPostStack.push(eventClazz);
        final int count;
        try {
            final List<EventData> eventDataList = optionalEventDataList.get();
            count = (int) eventDataList.stream()
                    .filter(eventData -> safeInvoke(eventData, iEvent))
                    .count();

            log.debug(Markers.GameLoopEventBus, "event post, event:{}, invoke count:{}",
                    () -> iEvent.getClass().getSimpleName(),
                    () -> count);
        } finally {
            eventPostStack.pop();
        }

        return count;
    }
}
