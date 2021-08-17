package org.gamedo.configuration;

import lombok.Data;
import org.gamedo.gameloop.components.entitymanager.GameLoopEntityManager;
import org.gamedo.gameloop.components.eventbus.GameLoopEventBus;
import org.gamedo.gameloop.components.scheduling.GameLoopScheduler;
import org.gamedo.gameloop.components.tickManager.GameLoopTickManager;
import org.gamedo.util.GamedoConfiguration;
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
     * 要关闭指标监控的IGameLoopGroup集合，填写gameLoopGroup的Id
     */
    Set<String> disabledGameLoopGroup = new HashSet<>();

    /**
     * 是否开启{@link GameLoopEntityManager}的指标采集
     */
    private boolean entityEnable = GamedoConfiguration.METRIC_ENTITY_ENABLE_DEFAULT;

    /**
     * 是否开启{@link GameLoopEventBus}的指标采集
     */
    private boolean eventEnable = GamedoConfiguration.METRIC_EVENT_ENABLE_DEFAULT;

    /**
     * 是否开启{@link GameLoopTickManager}的指标采集
     */
    private boolean tickEnable = GamedoConfiguration.METRIC_TICK_ENABLE_DEFAULT;

    /**
     * 是否开启{@link GameLoopScheduler}的指标采集
     */
    private boolean cronEnable = GamedoConfiguration.METRIC_CRON_ENABLE_DEFAULT;
}
