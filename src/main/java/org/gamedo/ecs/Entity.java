package org.gamedo.ecs;

import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.interfaces.IComponent;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.eventbus.event.EventPreRegisterEntity;
import org.gamedo.eventbus.event.EventPreUnregisterEntity;
import org.gamedo.eventbus.interfaces.IEventBus;
import org.gamedo.eventbus.interfaces.Subscribe;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.scheduling.interfaces.IScheduleRegister;
import org.gamedo.scheduling.interfaces.IScheduleRegisterFunction;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@EqualsAndHashCode(of = "id")
@Log4j2
public class Entity implements IEntity {
    private final String id;
    private final Map<Class<?>, Object> componentMap;
    private volatile IGameLoop belongedGameLoop;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Entity(final String id, Optional<Map<Class<? extends IComponent>, IComponent>> optionalMap) {
        this.id = id;
        componentMap = new HashMap<>(optionalMap.orElse(Collections.emptyMap()));
    }

    public Entity(String id) {
        this(id, Optional.empty());
    }

    public Entity(Supplier<String> idSupplier) {
        this(idSupplier.get());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Optional<IGameLoop> getBelongedGameLoop() {
        return Optional.ofNullable(belongedGameLoop);
    }

    @Override
    public void setBelongedGameLoop(IGameLoop belongedGameLoop) {

        if (this.belongedGameLoop != null) {
            final Optional<IEventBus> optionalIEventBus = this.belongedGameLoop.getComponent(IEventBus.class);
            if (optionalIEventBus.isPresent()) {
                final IEventBus iEventBus = optionalIEventBus.get();
                iEventBus.unregister(this);
                componentMap.values().forEach(o -> iEventBus.unregister(o));
            }
        }

        this.belongedGameLoop = belongedGameLoop;

        if (this.belongedGameLoop != null) {
            final Optional<IEventBus> optionalIEventBus = this.belongedGameLoop.getComponent(IEventBus.class);
            if (optionalIEventBus.isPresent()) {
                final IEventBus iEventBus = optionalIEventBus.get();
                iEventBus.register(this);
                componentMap.values().forEach(o -> iEventBus.register(o));
            }
        }
    }

    @Override
    public <T> boolean hasComponent(Class<T> clazz) {
        return componentMap.containsKey(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getComponent(Class<T> clazz) {
        return Optional.ofNullable((T) componentMap.get(clazz));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> addComponent(Class<T> clazz, T component) {
        final Optional<T> put = (Optional<T>) Optional.ofNullable(componentMap.put(clazz, component));

        getBelongedGameLoop().ifPresent(iGameLoop -> {
            final CompletableFuture<Integer> future = iGameLoop.submit(IScheduleRegisterFunction.registerSchedule(component));
            future.whenCompleteAsync((i, t) -> {
                if (t != null) {
                    log.error("exception caught on register schedule after adding component, clazz:" + clazz.getName(), t);
                } else {
                    log.debug("register schedule finish");
                }
            });
        });

        return put;
    }

    @Override
    public void tick(long elapse) {

    }

    @Subscribe
    public void eventPreRegisterEntity(final EventPreRegisterEntity event) {
        if (id.equals(event.getEntityId())) {
            final Optional<IScheduleRegister> optional = belongedGameLoop.getComponent(IScheduleRegister.class);
            optional.ifPresent(register -> {
                final Set<Object> components = new HashSet<>(componentMap.values());
                components.forEach(object -> register.register(object));
            });
        }
    }

    @Subscribe
    public void eventUnregisterEntity(final EventPreUnregisterEntity event) {
        if (id.equals(event.getEntityId())) {
            final Optional<IScheduleRegister> iScheduleRegister = belongedGameLoop.getComponent(IScheduleRegister.class);
            iScheduleRegister.ifPresent(register -> {
                final Set<Object> components = new HashSet<>(componentMap.values());
                components.forEach(object -> register.unregister(object.getClass()));
            });
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Entity{");
        sb.append("id='").append(id).append('\'');
        sb.append(", componentMap=").append(componentMap.keySet());
        sb.append('}');
        return sb.toString();
    }
}
