package de.nick1st.logging.filter;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static de.nick1st.logging.filter.Constants.MOD_NAME;

public class Filter extends AbstractFilter {

    public Config.LogLevel debugLevel = Config.LogLevel.NONE;

    public List<FilterPredicate> filters = new ArrayList<>();

    @Override
    public Result filter(LogEvent event) {
        for (FilterPredicate filter : filters) {
            if (filter.test(event)) {
                if (filter.logFiltered) {
                    Level level = Level.INFO;
                    try {
                        Level.valueOf(debugLevel.name());
                    } catch (IllegalArgumentException ignored) {}
                    Constants.LOG.atLevel(level).log("Filtered message of level {}", event.getLevel());
                }
                return Result.DENY;
            }
        }
        if (debugLevel != Config.LogLevel.NONE && !Objects.equals(event.getLoggerName(), MOD_NAME)) {
            logEvents(debugLevel, event);
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    private void logEvents(Config.LogLevel logEventsLevel, LogEvent event) {
        Level level = Level.INFO;
        try {
            Level.valueOf(logEventsLevel.name());
        } catch (IllegalArgumentException ignored) {}
        Throwable throwable = event.getMessage().getThrowable();
        Constants.LOG.atLevel(level).log("""
                Logging event:
                    - Level: {}
                    - Logger name: {}
                    - Thread name: {}
                    - Throwable class: {}
                    - Message: {}
                    - Format message: {}
                    - Parameter classes: {}
                """,
                event.getLevel().name(),
                event.getLoggerName(),
                event.getThreadName(),
                throwable != null ? throwable.getClass().getName() : null,
                event.getMessage().getFormattedMessage(),
                event.getMessage().getFormat(),
                Arrays.toString(Arrays.stream(event.getMessage().getParameters()).map(param -> param.getClass().getName()).toArray())
        );
    }
}
