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
     * 要关闭的指标监控的IGameLoopGroup集合
     */
    Set<String> disabledGameLoopGroup = new HashSet<>();
}
