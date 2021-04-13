package org.gamedo;

import lombok.extern.log4j.Log4j2;
import org.gamedo.application.GamedoApplicationComponentRegister;
import org.gamedo.configuration.EnableGamedoApplication;
import org.gamedo.ecs.interfaces.IApplication;
import org.gamedo.gameloop.GameLoopApplicationGroup;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Log4j2
@SpringBootApplication
@EnableGamedoApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    GamedoApplicationComponentRegister<IGameLoopGroup, GameLoopApplicationGroup> gameLoopGroup(IApplication gamedoapplication) {

        log.info("gameLoopGroupLoopGroup");

        final GameLoopApplicationGroup gameLoopGlobalGroup = new GameLoopApplicationGroup(gamedoapplication, "GameLoopGlobalGroup", 4);
        return new GamedoApplicationComponentRegister<>(IGameLoopGroup.class, gameLoopGlobalGroup);
    }
}
