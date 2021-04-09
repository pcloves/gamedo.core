package org.gamedo.scheduling.component;

import lombok.Value;
import org.apache.logging.log4j.Logger;
import org.gamedo.gameloop.interfaces.IGameLoop;

@Value
class ScheduleFunctionData {
    IGameLoop iGameLoop;
    String cron;
    Logger log;
}
