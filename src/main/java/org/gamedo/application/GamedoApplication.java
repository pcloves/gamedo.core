package org.gamedo.application;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IApplication;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.ResolvableType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public class GamedoApplication extends Entity implements IApplication, ApplicationListener<ApplicationEvent> {

    private static GamedoApplication App;
    private final AbstractApplicationContext applicationContext;

    public GamedoApplication(String id, AbstractApplicationContext applicationContext) {
        super(id);

        this.applicationContext = applicationContext;

        addComponent(ApplicationContext.class, applicationContext);
        addComponents(applicationContext);

        App = this;
    }

    @SuppressWarnings("unused")
    public static IApplication instance() {
        return App;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        log.debug("onApplicationEvent:{}", () -> event.getClass().getSimpleName());

        if (event instanceof ContextClosedEvent) {
            onContextClosedEvent((ContextClosedEvent) event);
        }
    }

    @SuppressWarnings("unused")
    private void onContextClosedEvent(ContextClosedEvent event) {

        log.info("ContextClosedEvent");
        componentMap.values()
                .parallelStream()
                .filter(o -> o instanceof IGameLoopGroup)
                .forEach(o -> {
                    final IGameLoopGroup gameLoopGroup = (IGameLoopGroup) o;
                    gameLoopGroup.shutdown();
                    log.info("IGameLoopGroup:{} shutdown", gameLoopGroup.getId());
                    try {
                        final boolean termination = gameLoopGroup.awaitTermination(60, TimeUnit.MILLISECONDS);
                        if (!termination) {
                            final List<Runnable> runnables = gameLoopGroup.shutdownNow();
                            log.warn("gameLoopGroup:{} shutdownNow, runnable list:{}", gameLoopGroup.getId(), runnables.size());
                        }
                    } catch (Exception exception) {
                        log.error("exception caught on await terminate, gameLoopGroup:" + gameLoopGroup.getId(), exception);
                        Thread.currentThread().interrupt();
                    }
                });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void addComponents(AbstractApplicationContext applicationContext) {

        final ResolvableType resolvableType = ResolvableType.forRawClass(ApplicationComponentRegister.class);
        final String[] beanNamesForType = applicationContext.getBeanNamesForType(resolvableType);
        final List<ApplicationComponentRegister> registerList = Arrays.stream(beanNamesForType)
                .map(s -> (ApplicationComponentRegister) applicationContext.getBean(s))
                .collect(Collectors.toList());

        registerList.forEach( rigster -> {
            final List<Class> componentClazzList = rigster.getClazzList();
            final Object instance = rigster.getObject();

            componentClazzList.forEach(clazz -> {
                final Optional component = addComponent(clazz, instance);
                if (component.isPresent()) {
                    log.error("duplicate componet add to {}, component type:{}",
                            GamedoApplication.class.getSimpleName(),
                            clazz.getName());

                    this.applicationContext.close();
                    System.exit(ExitCode.DuplicateApplicationComponent.code);
                }
            });
        });
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public boolean isInGameLoop() {
        return false;
    }
}
