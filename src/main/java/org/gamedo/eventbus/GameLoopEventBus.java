package org.gamedo.eventbus;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.eventbus.interfaces.IEvent;
import org.gamedo.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.eventbus.interfaces.Subscribe;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class GameLoopEventBus extends Component implements IGameLoopEventBus {
    private final Map<Class<? extends IEvent>, List<EventData>> classToEventDataMap = new HashMap<>(128);

    public GameLoopEventBus(IEntity owner) {
        super(owner);
    }

    private static boolean safeInvoke(EventData eventData, IEvent event) {
        final Method method = eventData.getMethod();
        try {
            ReflectionUtils.makeAccessible(method);
            method.invoke(eventData.getObject(), event);
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            final Class<? extends IEvent> eventClazz = event.getClass();
            log.error("exception caught, method:" + method.getName() + ", event:" + eventClazz.getName(), e);
        }

        return false;
    }

    @Override
    public int register(Object object) {

        final Class<?> objectClazz = object.getClass();
        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(objectClazz))
                .filter(method -> method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        return (int) annotatedMethodSet.stream()
                .filter(method -> method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic())
                .filter(method -> register(object, method))
                .count();
    }

    private boolean register(Object object, Method method) {
        if (!method.isAnnotationPresent(Subscribe.class)) {
            log.error("the method {} is not annotated by '{}'",
                    method.getName(),
                    Subscribe.class.getSimpleName());
            return false;
        }

        if (method.getParameterCount() != 1) {
            log.error("the method {} is required one parameter.", method.getName());
            return false;
        }

        final Class<?> eventClazz = method.getParameterTypes()[0];
        if (!IEvent.class.isAssignableFrom(eventClazz)) {
            log.error("the parameter type of method {} is not assignable from {}",
                    method.getName(),
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
            log.warn("multiply methods register on the same event:{}, object:{}, method list:{}",
                    eventClazz,
                    object.getClass(),
                    list);
        }

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
            log.error("the method {} is not annotated by '{}'",
                    method.getName(),
                    Subscribe.class.getSimpleName());
            return false;
        }

        if (method.getParameterCount() != 1) {
            log.error("the method {} is required one parameter.", method.getName());
        }

        final Class<?> eventClazz = method.getParameterTypes()[0];
        if (!IEvent.class.isAssignableFrom(eventClazz)) {
            log.error("the parameter type of method {} is not assignable from {}",
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
        return eventDataList.remove(eventData);
    }

    @Override
    public int post(IEvent iEvent) {

        final Class<? extends IEvent> eventClazz = iEvent.getClass();
        final Optional<List<EventData>> optionalList = Optional.ofNullable(classToEventDataMap.get(eventClazz));
        if (optionalList.isEmpty()) {
            return 0;
        }

        final List<EventData> eventDataList = optionalList.get();
        return (int) eventDataList.stream()
                .filter(eventData -> safeInvoke(eventData, iEvent))
                .count();
    }
}
