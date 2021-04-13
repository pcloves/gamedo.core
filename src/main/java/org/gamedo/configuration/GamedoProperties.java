package org.gamedo.configuration;

import lombok.Builder;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.concurrent.TimeUnit;

@ConfigurationProperties(prefix = "gamedo.core")
@Value
@ConstructorBinding
public class GamedoProperties {

    GameLoopGroupConfig gameLoop;

    @Value
    @ConstructorBinding
    @Builder
    public static class GameLoopGroupConfig {
        int delay;
        int period;
        TimeUnit timeUnit;
        boolean autoStart;

        public GameLoopGroupConfig(@DefaultValue("25") int delay,
                                   @DefaultValue("25") int period,
                                   @DefaultValue("MILLISECONDS") TimeUnit timeUnit,
                                   @DefaultValue("true") boolean autoStart) {
            this.delay = delay;
            this.period = period;
            this.timeUnit = timeUnit;
            this.autoStart = autoStart;
        }
    }
}
