package st.photonbur.Discord.Bot.lightbotv3.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class WarnFilter extends AbstractMatcherFilter {
    @Override
    public FilterReply decide(Object ev) {
        if (!isStarted()) return FilterReply.NEUTRAL;

        LoggingEvent loggingEvent = (LoggingEvent) ev;

        List<Level> eventsToKeep = Collections.singletonList(Level.WARN);
        return eventsToKeep.contains(loggingEvent.getLevel()) ? FilterReply.NEUTRAL : FilterReply.DENY;
    }
}
