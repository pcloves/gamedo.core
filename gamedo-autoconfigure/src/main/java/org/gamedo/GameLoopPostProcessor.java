package org.gamedo;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Marker;
import org.gamedo.annotation.CronOn;
import org.gamedo.annotation.SubscribeOn;
import org.gamedo.annotation.TickOn;
import org.gamedo.event.EventApplicationLifeCycle;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.gamedo.logging.Markers;
import org.gamedo.util.Pair;
import org.gamedo.util.function.GameLoopFunction;
import org.gamedo.util.function.IGameLoopEventBusFunction;
import org.gamedo.util.function.IGameLoopSchedulerFunction;
import org.gamedo.util.function.IGameLoopTickManagerFunction;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Log4j2
public class GameLoopPostProcessor implements BeanPostProcessor, ApplicationListener<ApplicationEvent>, PriorityOrdered {

    protected static final Map<String, IGameLoop> gameLoopMap = new ConcurrentHashMap<>();
    protected static final Map<String, IGameLoopGroup> gameLoopGroupMap = new ConcurrentHashMap<>();

    private final Set<Pair<TickOn, Object>> tickOnBeanSet = new HashSet<>();
    private final Set<Pair<SubscribeOn, Object>> subscribeOnBeanSet = new HashSet<>();
    private final Set<Pair<CronOn, Object>> cronOnBeanSet = new HashSet<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof IGameLoop) {
            final IGameLoop gameLoop = (IGameLoop) bean;
            final IGameLoop gameLoopOld = gameLoopMap.put(gameLoop.getId(), gameLoop);
            if (gameLoopOld != null && gameLoopOld != gameLoop) {
                log.error(Markers.GameLoop, "duplicate gameLoop created, old:{}, new:{}", gameLoopOld, gameLoop);
            }
        }

        if (bean instanceof IGameLoopGroup) {
            final IGameLoopGroup gameLoopGroup = (IGameLoopGroup) bean;
            final IGameLoopGroup gameLoopGroupOld = gameLoopGroupMap.put(gameLoopGroup.getId(), gameLoopGroup);
            if (gameLoopGroupOld != null && gameLoopGroupOld != gameLoopGroup) {
                log.error(Markers.GameLoop, "duplicate gameLoop created, old:{}, new:{}", gameLoopGroupOld, gameLoopGroup);
            }
        }

        final TickOn tickOn = AnnotationUtils.findAnnotation(bean.getClass(), TickOn.class);
        if (tickOn != null) {
            tickOnBeanSet.add(Pair.of(tickOn, bean));
        }

        final SubscribeOn subscribeOn = AnnotationUtils.findAnnotation(bean.getClass(), SubscribeOn.class);
        if (subscribeOn != null) {
            subscribeOnBeanSet.add(Pair.of(subscribeOn, bean));
        }

        final CronOn cronOn = AnnotationUtils.findAnnotation(bean.getClass(), CronOn.class);
        if (cronOn != null) {
            cronOnBeanSet.add(Pair.of(cronOn, bean));
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    private void register() {

        log.info(Markers.GameLoop, "begin register @TickOn/@SubscribeOn/@CronOn, gameLoop:{}, @TickOn:{}, " +
                "@SubscribeOn:{}, @CronOn:{}", gameLoopMap.size(), tickOnBeanSet.size(), subscribeOnBeanSet.size(), cronOnBeanSet.size()
        );

        tickOnBeanSet.parallelStream()
                .forEach(pair -> register(pair.getV(),
                        pair.getK().gameLoopId(),
                        IGameLoopTickManagerFunction.register(pair.getV()),
                        Markers.GameLoopTickManager));

        subscribeOnBeanSet.parallelStream()
                .forEach(pair -> register(pair.getK(),
                        pair.getK().gameLoopId(),
                        IGameLoopEventBusFunction.register(pair.getV()),
                        Markers.GameLoopEventBus));

        cronOnBeanSet.parallelStream()
                .forEach(pair -> register(pair.getK(),
                        pair.getK().gameLoopId(),
                        IGameLoopSchedulerFunction.register(pair.getV()),
                        Markers.GameLoopScheduler));
    }

    private void register(Object object, String gameLoopId, GameLoopFunction<Integer> registerFunction, Marker marker) {
        final IGameLoop gameLoop = gameLoopMap.get(gameLoopId);
        if (gameLoop == null) {
            log.error(marker, "register failed, get gameLoop failed, gameLoopId:{}, object:{}", gameLoopId, object.getClass().getName());
            return;
        }

        gameLoop.submit(registerFunction);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        final EventApplicationLifeCycle.Stage stage = EventApplicationLifeCycle.Stage.of(event.getClass());

        //检测是否关心该spring事件
        if (stage != EventApplicationLifeCycle.Stage.Unknown) {

            final Supplier<EventApplicationLifeCycle> lifeCycle = () -> new EventApplicationLifeCycle(stage);
            log.info(Markers.GamedoCore, "event post begin, event:{}", lifeCycle.get());

            //此时可以进行注册了
            if (stage == EventApplicationLifeCycle.Stage.Started) {
                register();
            }

            //将事件投递到所有的gameLoop
            final List<Pair<String, CompletableFuture<Integer>>> list = gameLoopMap.values()
                    .stream()
                    .map(iGameLoop -> Pair.of(iGameLoop.getId(), iGameLoop.submit(IGameLoopEventBusFunction.post(EventApplicationLifeCycle.class, lifeCycle))))
                    .collect(Collectors.toList());

            final CompletableFuture[] completableFutures1 = list.stream().map(Pair::getV).toArray(CompletableFuture[]::new);

            List<Object> resultList;
            try {
                //最多等5分钟
                CompletableFuture.allOf(completableFutures1).get(5, TimeUnit.MINUTES);
                resultList = list.stream()
                        .map(pair -> Pair.of(pair.getK(), pair.getV().getNow(0)))
                        .filter(pair -> pair.getV() > 0)
                        .collect(Collectors.toList());
            } catch (Exception exception) {
                log.error(Markers.GamedoCore, "exception caught.", exception);
                resultList = Collections.singletonList(list);
            }

            log.info(Markers.GamedoCore, "event post finish, handle event gameLoop:{}", resultList);
        }
    }
}
