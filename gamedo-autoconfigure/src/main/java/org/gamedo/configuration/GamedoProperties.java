package org.gamedo.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gamedo")
@Data
public class GamedoProperties {

    /**
     * 应用程序实例名称
     */
    private String name = "gamedo";
}
