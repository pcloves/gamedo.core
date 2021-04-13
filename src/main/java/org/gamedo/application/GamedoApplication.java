package org.gamedo.application;

import lombok.extern.log4j.Log4j2;
import org.gamedo.configuration.GamedoProperties;
import org.gamedo.ecs.Entity;
import org.gamedo.ecs.interfaces.IApplication;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
public class GamedoApplication extends Entity implements IApplication, ApplicationContextAware, ApplicationListener<ApplicationEvent> {

    private static final GamedoApplication App = new GamedoApplication();

    private GamedoProperties gamedoProperties;
    private AbstractApplicationContext applicationContext;

    private GamedoApplication() {
        super("GamedoApplication");
    }

    public static IApplication instance() {
        return App;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onApplicationEvent(ApplicationEvent event) {

        log.debug("onApplicationEvent:{}", () -> event.getClass().getSimpleName());

        if (event instanceof ApplicationStartedEvent) {
            final Map<String, GamedoApplicationComponentRegister> beansOfType = applicationContext.getBeansOfType(GamedoApplicationComponentRegister.class);
            beansOfType.forEach((s, rigster) -> {
                final Class componentClazz = rigster.getClazz();
                final Object instance = rigster.getObject();
                final Optional component = addComponent(componentClazz, instance);
                if (component.isPresent()) {
                    log.error("duplicate componet add to {}, component type:{}, bean name:{}",
                            GamedoApplication.class.getSimpleName(),
                            s,
                            componentClazz.getName());

                    applicationContext.close();
                    System.exit(ExitCode.DuplicateApplicationComponent.code);
                }
            });

        }

        if (event instanceof ContextClosedEvent) {
            componentMap.values()
                    .parallelStream()
                    .filter(o -> o instanceof IGameLoopGroup)
                    .forEach(o -> {
                        final IGameLoopGroup gameLoopGroup = (IGameLoopGroup) o;
                        gameLoopGroup.shutdown();
                        try {
                            final boolean termination = gameLoopGroup.awaitTermination(60, TimeUnit.MILLISECONDS);
                            if (!termination) {
                                gameLoopGroup.shutdownNow();
                            }
                        } catch (Exception exception) {
                            log.info("exception caught on await terminate", exception);
                            Thread.currentThread().interrupt();
                        }
                    });
        }
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {

        log.debug("setApplicationContext");

        this.applicationContext = (AbstractApplicationContext) applicationContext;
        gamedoProperties = this.applicationContext.getBean(GamedoProperties.class);

        addComponent(ApplicationContext.class, applicationContext);
    }

    @Override
    public int runAllGameLoopGroupComponent() {

        return (int) componentMap.values()
                .stream()
                .filter(o -> o instanceof IGameLoopGroup)
                .mapToLong(o -> ((IGameLoopGroup) o).run(50/*TODO:use configuration*/, 50, TimeUnit.MILLISECONDS).size())
                .sum();
    }

    @Override
    public boolean isInGameLoop() {
        return false;
    }
}
