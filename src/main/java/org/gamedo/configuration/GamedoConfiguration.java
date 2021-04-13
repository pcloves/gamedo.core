package org.gamedo.configuration;

import org.gamedo.application.GamedoApplication;
import org.gamedo.ecs.interfaces.IApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GamedoProperties.class)
public class GamedoConfiguration {

    @Bean
    IApplication gamedoApplication() {
        return GamedoApplication.instance();
    }
}
