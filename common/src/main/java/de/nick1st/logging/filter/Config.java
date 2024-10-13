package de.nick1st.logging.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Config {
    public LogLevel logEvents = LogLevel.NONE;
    public List<FilterRule> rules;

    /**
     * Comparable are combined by AND
     * StringComparable are combined by OR
     * Map entries are combined by AND
     */
    public static class FilterRule {
        public List<Comparable<LogLevel>> level = new ArrayList<>();
        public List<StringComparable> loggerName = new ArrayList<>();
        public List<StringComparable> threadName = new ArrayList<>();
        public List<StringComparable> throwableClass = new ArrayList<>();
        public List<StringComparable> message = new ArrayList<>();
        public List<StringComparable> formatMessage = new ArrayList<>();
        public List<Map<Integer, StringComparable>> parameterClasses = new ArrayList<>();
        public List<Map<Integer, StringComparable>> parameterValues = new ArrayList<>();
        public boolean logRuleFiring = false;
    }

    public static class StringComparable {
        public Relation relation;
        public String value;
        public List<StringComparable> whitelist = new ArrayList<>();

        public static enum Relation {
            MATCH,
            STARTS_WITH,
            CONTAINS,
            ENDS_WITH,
            MATCH_IGNORE_CASE,
            STARTS_WITH_IGNORE_CASE,
            CONTAINS_IGNORE_CASE,
            ENDS_WITH_IGNORE_CASE,
            REGEX
        }
    }

    public static class Comparable<T> {
        public Relation relation;
        public T value;

        public static enum Relation {
            LESS_THAN,
            LESS_THAN_OR_EQUAL,
            EQUAL,
            GREATER_THAN_OR_EQUAL,
            GREATER_THAN,
            NOT_EQUAL
        }
    }

    public static enum LogLevel {
        ALL,
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL,
        NONE;

        public static LogLevel convert(org.apache.logging.log4j.Level level) {
            if (level == org.apache.logging.log4j.Level.ALL) return ALL;
            if (level == org.apache.logging.log4j.Level.TRACE) return TRACE;
            if (level == org.apache.logging.log4j.Level.DEBUG) return DEBUG;
            if (level == org.apache.logging.log4j.Level.INFO) return INFO;
            if (level == org.apache.logging.log4j.Level.WARN) return WARN;
            if (level == org.apache.logging.log4j.Level.ERROR) return ERROR;
            if (level == org.apache.logging.log4j.Level.FATAL) return FATAL;
            return NONE;
        }
    }
}
