package org.gamedo.gameloop.components.eventbus;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.gamedo.annotation.Subscribe;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.components.eventbus.interfaces.IFilterableEvent;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Log4j2
public class GameLoopEventBus extends GameLoopComponent implements IGameLoopEventBus {

    private static final Function<Class<? extends IEvent>, List<EventData>> EventDataListFunction = eventClazz1 -> new ArrayList<>(32);
    private long counter = 0L;
    private final Map<Class<? extends IEvent>, List<EventData>> eventClazzName2EventDataMap = new HashMap<>(128);
    private final Deque<Class<?>> eventPostStack = new LinkedList<>();
    private final Map<String, Pair<AtomicLong, Gauge>> eventClazzName2GaugeMap = new HashMap<>(128);

    public GameLoopEventBus(IGameLoop owner) {
        super(owner);
    }

    private boolean safeInvoke(EventData eventData, IEvent event) {
        final Object object = eventData.getObject();
        final Method method = eventData.getMethod();
        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopEventBus, "the {} hasn't a owner yet.", GameLoopEventBus.class.getSimpleName());
            return false;
        }

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

        return Boolean.TRUE.equals(timer.record(() -> {
            try (final GamedoLogContext.CloseableEntityId ignored = GamedoLogContext.pushEntityIdAuto(object)) {
                method.invoke(object, event);
                return true;
            } catch (Exception e) {
                final Class<? extends IEvent> eventClazz = event.getClass();
                log.atLevel(Level.ERROR)
                        .withThrowable(e)
                        .withMarker(Markers.GameLoopEventBus)
                        .log("exception caught, class:{}, method:{}, event:{}",
                                object.getClass().getName(),
                                method.getName(),
                                eventClazz.getName());
            }

            return false;
        }));
    }

    @Override
    public int register(Object object) {

        final Class<?> clazz = object.getClass();
        final List<Pair<Method, Subscribe>> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .map(method -> Pair.of(method, method.getAnnotation(Subscribe.class)))
                .filter(pair -> pair.getV() != null)
                .collect(Collectors.toList());

        if (annotatedMethodSet.isEmpty()) {
            log.info(Markers.GameLoopEventBus, "none annotation {} method found, clazz:{}",
                    Subscribe.class.getSimpleName(),
                    clazz.getName());
            return 0;
        }


        final int count = (int) annotatedMethodSet.stream()
                .filter(pair -> register(object, pair.getK(), pair.getV().value()))
                .count();

        log.debug(Markers.GameLoopEventBus, "register eventBus finish, clazz:{}, totalCount:{}, successCount:{}",
                clazz.getSimpleName(),
                annotatedMethodSet.size(),
                count
        );

        return count;
    }

    @Override
    public boolean register(Object object, Method method, short priority) {
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
        return register(object, method, (Class<? extends IEvent>) eventClazz, priority);
    }

    private <T extends IEvent> boolean register(Object object, Method method, Class<T> eventClazz, short priority) {
        final List<EventData> eventDataList = eventClazzName2EventDataMap.computeIfAbsent(eventClazz, EventDataListFunction);
        //如果优先级相同，那就看谁先注册
        final long compareValue = ((long) priority << 48) + counter++;
        final EventData eventData = new EventData(object, method, compareValue);
        if (eventDataList.contains(eventData)) {
            log.warn(Markers.GameLoopEventBus, "the event has registered, event clazz:{}, object clazz:{}, " +
                            "method:{}",
                    eventClazz.getSimpleName(),
                    object.getClass().getSimpleName(),
                    method.getName());
            return false;
        }

        ReflectionUtils.makeAccessible(method);

        int index = Collections.binarySearch(eventDataList, eventData, Comparator.comparingLong(EventData::getCompareValue));
        //排序插入
        eventDataList.add(index < 0 ? ~index : index, eventData);

        final List<EventData> duplicateEventDataList = eventDataList.stream()
                .filter(eventData1 -> eventData1.getObject() == object)
                .collect(Collectors.toList());

        if (duplicateEventDataList.size() > 1) {
            final List<Method> list = duplicateEventDataList.stream()
                    .map(EventData::getMethod)
                    .collect(Collectors.toList());
            log.warn(Markers.GameLoopEventBus, "multiply methods register on the same event:{}, object:{}, " +
                    "method list:{}", eventClazz, object.getClass(), list);
        }

        log.debug(Markers.GameLoopEventBus, "register, event clazz:{}, object clazz:{}, method:{}, result:{}",
                eventClazz::getSimpleName,
                () -> object.getClass().getSimpleName(),
                method::getName,
                () -> true
        );

        metricGauge(eventClazz, eventDataList);

        return true;
    }

    @Override
    public int unregister(Object object) {

        final Class<?> objectClazz = object.getClass();
        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(objectClazz))
                .filter(method -> method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        return (int) annotatedMethodSet.stream()
                .filter(method -> unregister(object, method))
                .count();
    }

    private boolean unregister(Object object, Method method) {

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

        final List<EventData> eventDataList = eventClazzName2EventDataMap.computeIfAbsent(eventClazz, EventDataListFunction);

        final EventData eventData = new EventData(object, method);
        final boolean remove = eventDataList.remove(eventData);

        log.debug(Markers.GameLoopEventBus, "unregister, event clazz:{}, object clazz:{}, method:{}, result:{}",
                eventClazz::getSimpleName,
                () -> object.getClass().getSimpleName(),
                method::getName,
                () -> remove);

        metricGauge(eventClazz, eventDataList);

        return remove;
    }

    private <T extends IEvent> void metricGauge(Class<T> eventClazz, List<EventData> eventDataList) {
        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopEventBus, "the {} hasn't a owner yet.", GameLoopEventBus.class.getSimpleName());
            return;
        }
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

    @SuppressWarnings("unchecked")
    @Override
    public int post(IEvent iEvent) {
        return post((Class<IEvent>) iEvent.getClass(), () -> iEvent);
    }

    @Override
    public <T extends IEvent> int post(Class<T> eventClazz, Supplier<T> eventSupplier) {
        final Optional<List<EventData>> optionalEventDataList = Optional.ofNullable(eventClazzName2EventDataMap.get(eventClazz));
        if (optionalEventDataList.isEmpty()) {
            return 0;
        }

        final T iEvent = eventSupplier.get();
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
            //处理事件的过程中很有可能触发新的注册或反注册的行为，这会导致optionalEventDataList被增删，因此这里偷一下懒，直接使用一个新列表进行迭代
            count = (int) new ArrayList<>(optionalEventDataList.get())
                    .stream()
                    .filter(eventData -> eventFilter(eventData, iEvent))
                    .filter(eventData -> safeInvoke(eventData, iEvent))
                    .count();

            log.debug(Markers.GameLoopEventBus, "event post, eventClazz:{}, invoke count:{}, event:{}",
                    eventClazz::getSimpleName,
                    () -> count,
                    () -> iEvent);
        } finally {
            eventPostStack.pop();
        }

        return count;
    }

    @SuppressWarnings({"MethodMayBeStatic", "rawtypes", "unchecked"})
    private boolean eventFilter(EventData eventData, IEvent iEvent) {

        try {
            if (iEvent instanceof IFilterableEvent) {
                final IFilterableEvent filterableEvent = (IFilterableEvent) iEvent;
                //当且仅当订阅者的类型满足需求，才进行过滤检测，否则就表示类型不对，直接检测失败
                return filterableEvent.filter(eventData, filterableEvent);
            }

            return true;
        } catch (Exception e) {
            final Class<? extends IEvent> eventClazz = iEvent.getClass();
            log.error(Markers.GameLoopEventBus, "exception caught when filter, event:" + eventClazz.getName(), e);
            return false;
        }
    }
}
