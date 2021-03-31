package org.gamedo.event;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.event.interfaces.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@ToString
public class EventBus extends Component implements IEventBus {
    private static long index;

    private final Map<Class<?>, List<EventHandlerData<?>>> eventType2HandlerDataListMap = new HashMap<>(512);
    private final Map<IEventHandler<?>, EventHandlerData<?>> eventHandler2HandlerDataMap = new HashMap<>(2048);
    private final Queue<IEvent> cachedEventQueue = new ConcurrentLinkedQueue<>();
    private final Map<Class<?>, Set<IEventFilter<IEvent>>> eventType2EventFilterSetMap = new HashMap<>(512);

    public EventBus(IEntity owner) {
        super(owner);
    }

    @Override
    public <E extends IEvent> boolean registerEvent(final Class<E> eventType,
                                                       final IEventHandler<E> eventHandler,
                                                       final EventPriority priority) {
        if (eventHandler2HandlerDataMap.containsKey(eventHandler)) {
            return false;
        }

        final List<EventHandlerData<?>> handlerDataList =
                eventType2HandlerDataListMap.computeIfAbsent(eventType, k -> new LinkedList<>());
        final EventHandlerData<E> handlerData = new EventHandlerData<E>(priority, eventHandler);

        handlerDataList.add(handlerData);
        handlerDataList.sort(Comparator.comparing(o -> o.priority));
        eventHandler2HandlerDataMap.put(eventHandler, handlerData);

        return true;
    }

    @Override
    public <E extends IEvent> void unRegisterEvent(Class<E> eventType, IEventHandler<E> eventHandler) {
        @SuppressWarnings("unchecked") final EventHandlerData<E> handlerData = (EventHandlerData<E>) eventHandler2HandlerDataMap.get(eventHandler);
        if (handlerData == null) {
            return;
        }
        eventHandler2HandlerDataMap.remove(eventHandler);

        final List<EventHandlerData<?>> handlerDataList = eventType2HandlerDataListMap.get(eventType);
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
    public <E extends IEvent> int sendEvent(final E event) {
        final Class<? extends IEvent> eventType = event.getClass();
        final List<EventHandlerData<?>> handlerDataList = eventType2HandlerDataListMap.get(eventType);
        if (handlerDataList == null) {
            return 0;
        }

        int hanleCount = 0;
        for (EventHandlerData<?> handlerData : handlerDataList) {
            try {
                @SuppressWarnings("unchecked") final IEventHandler<E> handler = (IEventHandler<E>) handlerData.handler;
                final Set<IEventFilter<IEvent>> eventFilterSet = Optional.ofNullable(eventType2EventFilterSetMap.get(eventType)).orElse(Collections.emptySet());
                final boolean pass = eventFilterSet
                        .stream()
                        .allMatch(filter -> filter.test(event));

                if (pass) {
                    handler.accept(event);
                    hanleCount++;
                }
            } catch (Exception e) {
                log.error("exception caught", e);
            }
        }

        return hanleCount;
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

    private static class EventHandlerData<E extends IEvent> {
        private final long priority;
        private final IEventHandler<E> handler;

        private EventHandlerData(final EventPriority priority, final IEventHandler<E> handler) {
            this.priority = ((long) priority.ordinal() << 60) + index++;
            this.handler = handler;
        }
    }
}
