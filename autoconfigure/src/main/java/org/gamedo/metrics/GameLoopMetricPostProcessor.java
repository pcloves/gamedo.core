package org.gamedo.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.extern.log4j.Log4j2;
import org.gamedo.configuration.MetricProperties;
import org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.Markers;
import org.gamedo.util.Metric;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

            final Tags tags = Metric.tags(iGameLoop);

            Gauge.builder(Metric.MetricNameEntity, () -> getEntityCountMap(gameLoop, tags, true).values()
                            .stream()
                            .mapToLong(Long::longValue)
                            .sum())
                    .description("the IEntity count in the IGameLoop")
                    .baseUnit(BaseUnits.OBJECTS)
                    .tags(tags)
                    .tag("type", "All")
                    .register(meterRegistry);

            return true;
        });
    }

    private Map<String, Long> getEntityCountMap(IGameLoop iGameLoop, Tags tags, boolean addSubMetric) {

        try {
            log.debug(Markers.GamedoMetrics,
                    "getEntityCountMap begin, gameLoop:{}, addSubMetric:{}",
                    () -> iGameLoop.getId(),
                    () -> addSubMetric);

            final Map<String, Long> map = iGameLoop.submit(iGameLoop1 -> iGameLoop1.getComponent(IGameLoopEntityManager.class)
                            .map(manager -> manager.getEntityMap()
                                    .values()
                                    .stream()
                                    .collect(Collectors.groupingBy(entity -> entity
                                            .getClass()
                                            .getSimpleName()
                                            , Collectors.counting())))
                            .orElse(Collections.emptyMap()))
                    .get(10, TimeUnit.SECONDS);

            if (addSubMetric) {
                map.forEach((clazz, count) -> {

                    final Supplier<Number> supplier = () -> getEntityCountMap(iGameLoop, tags, false)
                            .getOrDefault(clazz, 0L);

                    Gauge.builder(Metric.MetricNameEntity, supplier)
                            .description("the IEntity count in the IGameLoop")
                            .baseUnit(BaseUnits.OBJECTS)
                            .tags(tags)
                            .tag("type", clazz)
                            .register(meterRegistry);
                });
            }

            log.debug(Markers.GamedoMetrics,
                    "getEntityCountMap finish, gameLoop:{}, addSubMetric:{}",
                    () -> iGameLoop.getId(),
                    () -> addSubMetric);
            return map;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(Markers.GamedoMetrics, "exception caught.", e);
            return Collections.emptyMap();
        }
    }
}
