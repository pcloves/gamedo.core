package org.gamedo.gameloop.components.eventbus;

import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.GamedoComponent;
import org.gamedo.annotation.Subscribe;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.GamedoLogContext;
import org.gamedo.logging.Markers;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@GamedoComponent
public class GameLoopEventBus extends GameLoopComponent implements IGameLoopEventBus {
    public static final int MAX_EVENT_POST_DEPTH_DEFAULT = 20;
    private static final int MAX_EVENT_POST_DEPTH = Integer.getInteger("gamedo.gameloop.max-event-post-depth", MAX_EVENT_POST_DEPTH_DEFAULT);
    private final Map<Class<? extends IEvent>, List<EventData>> classToEventDataMap = new HashMap<>(128);
    private final Deque<Class<?>> eventPostStack = new LinkedList<>();

    public GameLoopEventBus(IGameLoop owner) {
        super(owner);
    }

    private static boolean safeInvoke(EventData eventData, IEvent event) {
        final Method method = eventData.getMethod();

        try (final GamedoLogContext.CloseableEntityId ignored = GamedoLogContext.pushEntityIdAuto(eventData.getObject())) {
            ReflectionUtils.makeAccessible(method);
            method.invoke(eventData.getObject(), event);
            return true;
        } catch (Throwable e) {
            final Class<? extends IEvent> eventClazz = event.getClass();
            log.error(Markers.GameLoopEventBus, "exception caught, method:" + method.getName() +
                    ", event:" + eventClazz.getName(), e);
        }

        return false;
    }

    @Override
    public int register(Object object) {

        final Class<?> clazz = object.getClass();
        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.warn(Markers.GameLoopEventBus, "the Object has none annotated method, annotation:{}, clazz:{}",
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
        final List<EventData> eventDataList = classToEventDataMap.computeIfAbsent(eventClazz, function);

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

        final List<EventData> eventDataList1 = eventDataList.stream()
                .filter(eventData1 -> eventData1.getObject() == object)
                .collect(Collectors.toList());

        if (eventDataList1.size() > 1) {
            final List<Method> list = eventDataList1.stream()
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
        final List<EventData> eventDataList = classToEventDataMap.computeIfAbsent(eventClazz, function);

        final EventData eventData = new EventData(object, method);
        final boolean remove = eventDataList.remove(eventData);

        log.debug(Markers.GameLoopEventBus, "unregister, event clazz:{}, object clazz:{}, method:{}, result:{}",
                () -> eventClazz.getSimpleName(),
                () -> object.getClass().getSimpleName(),
                () -> method.getName(),
                () -> remove);

        return remove;
    }

    @Override
    public int post(IEvent iEvent) {

        final Class<? extends IEvent> eventClazz = iEvent.getClass();
        final Optional<List<EventData>> optionalEventDataList = Optional.ofNullable(classToEventDataMap.get(eventClazz));
        if (optionalEventDataList.isEmpty()) {
            return 0;
        }

        if (eventPostStack.size() > MAX_EVENT_POST_DEPTH) {
            final List<String> eventClazzList = eventPostStack.stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.toList());
            log.error(Markers.GameLoopEventBus,
                    "post event overflow, max depth:{}, current stack:{}",
                    MAX_EVENT_POST_DEPTH,
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
