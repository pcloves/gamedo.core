package org.gamedo.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gamedo.core")
@Data
public class GamedoProperties {

    /**
     * 应用程序的id
     */
    String applicationId;
}
