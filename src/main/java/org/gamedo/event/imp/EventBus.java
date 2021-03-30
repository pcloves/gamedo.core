package org.gamedo.event.imp;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.impl.Component;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.event.interfaces.EventPriority;
import org.gamedo.event.interfaces.IEvent;
import org.gamedo.event.interfaces.IEventBus;
import org.gamedo.event.interfaces.IEventFilter;
import org.gamedo.event.interfaces.IEventHandler;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@ToString
public class EventBus extends Component implements IEventBus {
    private static long index;

    private final Map<Class<?>, List<EventHandlerData<?, ?>>> eventType2HandlerDataListMap = new HashMap<>(512);
    private final Map<IEventHandler<?, ?>, EventHandlerData<?, ?>> eventHandler2HandlerDataMap = new HashMap<>(2048);
    private final Queue<IEvent> cachedEventQueue = new ConcurrentLinkedQueue<>();
    private final Map<Class<?>, Set<IEventFilter<IEvent>>> eventType2EventFilterSetMap = new HashMap<>(512);

    public EventBus(IEntity owner) {
        super(owner);
    }

    @Override
    public <E extends IEvent, R> boolean registerEvent(final Class<E> eventType,
                                                    final IEventHandler<E, R> eventHandler,
                                                    final EventPriority priority) {

        @SuppressWarnings("unchecked")
        final Class<? extends IEventHandler<E, R>> eventHandlerClass = (Class<? extends IEventHandler<E, R>>) eventHandler.getClass();
        final int modifiers = eventHandlerClass.getModifiers();
        if (!Modifier.isFinal(modifiers)) {
//            log.error("the param IEventHandler<E> must be defined with final.");
            return false;
        }

        if (eventHandler2HandlerDataMap.containsKey(eventHandler)) {
            return false;
        }

        final List<EventHandlerData<?, ?>> handlerDataList =
                eventType2HandlerDataListMap.computeIfAbsent(eventType, k -> new LinkedList<>());
        final EventHandlerData<E, R> handlerData = new EventHandlerData<E, R>(priority, eventHandler);

        handlerDataList.add(handlerData);
        handlerDataList.sort(Comparator.comparing(o -> o.priority));
        eventHandler2HandlerDataMap.put(eventHandler, handlerData);

        return true;
    }

    @Override
    public <E extends IEvent, R> void unRegisterEvent(Class<E> eventType, IEventHandler<E, R> eventHandler) {
        @SuppressWarnings("unchecked")
        final EventHandlerData<E, R> handlerData = (EventHandlerData<E, R>) eventHandler2HandlerDataMap.get(eventHandler);
        if (handlerData == null) {
            return;
        }
        eventHandler2HandlerDataMap.remove(eventHandler);

        final List<EventHandlerData<?, ?>> handlerDataList = eventType2HandlerDataListMap.get(eventType);
        if (handlerDataList == null) {
            return;
        }
        handlerDataList.remove(handlerData);
    }

    @Override
    public <E extends IEvent> void addEventFilter(Class<E> eventType, IEventFilter<IEvent> eventFilter) {
        Set<IEventFilter<IEvent>> eventFilterSet = eventType2EventFilterSetMap.computeIfAbsent(eventType, k -> new HashSet<>(10));
        eventFilterSet.add(eventFilter);
    }

    @Override
    public <E extends IEvent> void removeEventFilter(final Class<E> eventType, final IEventFilter<E> eventFilter) {
        Set<IEventFilter<IEvent>> eventFilterSet = eventType2EventFilterSetMap.get(eventType);
        if (eventFilterSet == null) {
            return;
        }

        eventFilterSet.remove(eventFilter);
    }

    @Override
    public <E extends IEvent, R> Optional<R> sendEvent(final E event) {
        final Class<? extends IEvent> eventType = event.getClass();
        final List<EventHandlerData<?, ?>> handlerDataList = eventType2HandlerDataListMap.get(eventType);
        if (handlerDataList == null) {
            return Optional.empty();
        }

        for (EventHandlerData<?, ?> handlerData : handlerDataList) {
            try {
                @SuppressWarnings("unchecked")
                final IEventHandler<E, R> handler = (IEventHandler<E, R>) handlerData.handler;

                boolean isFilter = false;
                final Set<IEventFilter<IEvent>> eventFilterSet = eventType2EventFilterSetMap.get(eventType);
                if (eventFilterSet != null) {
                    for (IEventFilter<IEvent> eventFilter : eventFilterSet) {
                        if (!eventFilter.test(event)) {
                            isFilter = true;
//                            log.info("event is filtered, event:{}", event.getClass().getSimpleName());
                            break;
                        }
                    }
                }

                if (!isFilter) {
                    return Optional.ofNullable(handler.apply(event));
                }
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    @Override
    public <E extends IEvent> void postEvent(final E event) {
        cachedEventQueue.add(event);
    }

    @Override
    public int dispatchCachedEvent(final int maxDispatchCount) {
        int dispatchCount = Math.min(cachedEventQueue.size(), maxDispatchCount);
        while (dispatchCount > 0) {
            dispatchCount--;

            final IEvent event = cachedEventQueue.poll();
            sendEvent(event);
        }

        return 0;
    }

    @Override
    public String getOwnerId() {
        return null;
    }

    private static class EventHandlerData<E extends IEvent, R> {
        private final long priority;
        private final IEventHandler<E, R> handler;

        private EventHandlerData(final EventPriority priority, final IEventHandler<E, R> handler) {
            this.priority = ((long) priority.ordinal() << 60) + index++;
            this.handler = handler;
        }
    }
}
