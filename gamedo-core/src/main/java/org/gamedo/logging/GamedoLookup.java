package org.gamedo.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;

@SuppressWarnings({"SwitchStatementWithTooFewBranches", "unused"})
@Plugin(name = "gamedo", category = StrLookup.CATEGORY)
public class GamedoLookup extends AbstractLookup {

    @Override
    public String lookup(LogEvent event, String key) {

        switch (key) {
            case "entityId" :
                //取栈顶上的id，也就是最新的id
                return GamedoLogContext.ENTITY_ID_STACK.get().isEmpty() ? "null" : GamedoLogContext.ENTITY_ID_STACK.get().peek();
            default:
                throw new IllegalArgumentException(key);
        }
    }
}
