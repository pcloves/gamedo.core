package org.gamedo;

import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class Application {
    private static IGameLoopGroup worker;
    private static IGameLoopGroup io;
    private static IGameLoopGroup single;

    private static ApplicationContext applicationContext;

    private Application(ApplicationContext applicationContext, GameLoopProperties applicationProperties) {
        Application.applicationContext = applicationContext;

        worker = applicationContext.getBean(IGameLoopGroup.class, applicationProperties.getGameLoopConfigWorker());
        io = applicationContext.getBean(IGameLoopGroup.class, applicationProperties.getGameLoopConfigIo());
        single = applicationContext.getBean(IGameLoopGroup.class, applicationProperties.getGameLoopConfigSingle());
    }

    public static ApplicationContext context() {
        return applicationContext;
    }

    public static IGameLoopGroup worker() {
        return worker;
    }

    public static IGameLoopGroup io() {
        return io;
    }

    public static IGameLoopGroup single() {
        return single;
    }
}
