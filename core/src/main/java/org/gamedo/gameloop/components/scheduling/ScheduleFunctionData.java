package org.gamedo.gameloop.components.scheduling;

import lombok.Value;
import org.apache.logging.log4j.Logger;

@Value
class ScheduleFunctionData {
    GameLoopScheduler scheduleRegister;
    String cron;
    Logger log;
}
