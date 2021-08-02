package org.gamedo.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "gamedo.metrics")
@Data
public class MetricProperties {

    /**
     * 指标监控总开关
     */
    boolean enable;

    /**
     * 要关闭指标监控的IGameLoopGroup集合
     */
    Set<String> disabledGameLoopGroup = new HashSet<>();

    /**
     * 要关闭指标监控的IGameLoop组件集合，需要填入IGameLoop组件的类全限定名，例如：
     * org.gamedo.gameloop.components.entitymanager.interfaces.IGameLoopEntityManager
     */
    Set<String> disabledGameLoopComponent = new HashSet<>();
}
