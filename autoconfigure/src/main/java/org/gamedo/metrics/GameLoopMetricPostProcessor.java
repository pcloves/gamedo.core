package org.gamedo.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.extern.log4j.Log4j2;
import org.gamedo.configuration.MetricProperties;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.util.function.IGameLoopEntityManagerFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.ToDoubleFunction;

@Component
@Log4j2
public class GameLoopMetricPostProcessor implements BeanPostProcessor {

    private final MeterRegistry meterRegistry;
    private final MetricProperties metricProperties;
    private final Set<IGameLoop> gameLoopSet = new HashSet<>();

    public GameLoopMetricPostProcessor(MeterRegistry meterRegistry, MetricProperties metricProperties) {
        this.meterRegistry = meterRegistry;
        this.metricProperties = metricProperties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {

        if (bean instanceof IGameLoop) {
            gameLoopSet.add((IGameLoop) bean);
        }

        return bean;
    }

    @SuppressWarnings("unused")
    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady(ApplicationReadyEvent event) {

        if (metricProperties.isEnable()) {
            gameLoopSet.forEach(this::metricEntity);
        }
    }

    private void metricEntity(IGameLoop gameLoop) {

        if (metricProperties.getDisabledGameLoopComponent().contains(IGameLoopEntityManager.class.getName())) {
            return;
        }

        gameLoop.submit(iGameLoop -> {

            final Optional<IGameLoopEntityManager> component = iGameLoop.getComponent(IGameLoopEntityManager.class);
            final Tags tags = iGameLoop.owner()
                    .map(iGameLoopGroup -> Tags.of("name", iGameLoop.getId(), "owner", iGameLoopGroup.getId()))
                    .orElse(Tags.of("name", iGameLoop.getId()));

            return component.map(entityManager -> {
                Gauge.builder("gamedo.gameloop.entity", iGameLoop, getEntityCount())
                        .description("the IEntity count of the IGameLoop")
                        .baseUnit(BaseUnits.OBJECTS)
                        .tags(tags)
                        .register(meterRegistry);
                return true;
            }).orElse(false);
        });
    }

    private static ToDoubleFunction<IGameLoop> getEntityCount() {
        return iGameLoop -> {
            try {
                //需要安全发布到IGameLoop线程
                return iGameLoop.submit(IGameLoopEntityManagerFunction.getEntityCount())
                        .get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                return -1;
            }
        };
    }
}
