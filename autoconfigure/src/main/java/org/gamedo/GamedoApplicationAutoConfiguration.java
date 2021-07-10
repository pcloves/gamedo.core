package org.gamedo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GamedoApplicationProperties.class)
public class GamedoApplicationAutoConfiguration {


}
