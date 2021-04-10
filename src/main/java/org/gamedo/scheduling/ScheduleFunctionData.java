package org.gamedo.scheduling;

import lombok.Value;
import org.apache.logging.log4j.Logger;

@Value
class ScheduleFunctionData {
    Scheduler scheduleRegister;
    String cron;
    Logger log;
}
