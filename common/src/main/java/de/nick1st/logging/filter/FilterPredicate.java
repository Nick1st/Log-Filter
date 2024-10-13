package de.nick1st.logging.filter;

import org.apache.logging.log4j.core.LogEvent;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static de.nick1st.logging.filter.Config.LogLevel.convert;

public class FilterPredicate implements Predicate<LogEvent> {
    private final AndPredicate<LogEvent> predicate = new AndPredicate<>();
    public final boolean logFiltered;

    public FilterPredicate(Config.FilterRule rule) {
        predicate.add(event -> event.getLoggerName() != Constants.MOD_NAME); // Guard against processing our own logging
        predicate.add(createLevelPredicate(rule));
        predicate.add(createStringMatchPredicates(rule.loggerName, LogEvent::getLoggerName, false));
        predicate.add(createStringMatchPredicates(rule.threadName, LogEvent::getThreadName, false));
        predicate.add(createStringMatchPredicates(rule.throwableClass, event -> event.getMessage().getThrowable().getClass().getName(), false));
        predicate.add(createStringMatchPredicates(rule.message, event -> event.getMessage().getFormattedMessage(), false));
        predicate.add(createStringMatchPredicates(rule.formatMessage, event -> event.getMessage().getFormat(), false));
        predicate.add(createStringMapMatchPredicate(rule.parameterClasses, (event, integer) -> event.getMessage().getParameters()[integer].getClass().getName()));
        predicate.add(createStringMapMatchPredicate(rule.parameterValues, (event, integer) -> Objects.toString(event.getMessage().getParameters()[integer])));
        logFiltered = rule.logRuleFiring;
    }

    private Predicate<LogEvent> createLevelPredicate(Config.FilterRule rule) {
        AndPredicate<LogEvent> levelPredicate = new AndPredicate<>();
        rule.level.forEach(logLevelComparable -> {
            Predicate<LogEvent> p = switch (logLevelComparable.relation) {
                case EQUAL -> event -> convert(event.getLevel()).ordinal() == logLevelComparable.value.ordinal();
                case LESS_THAN -> event -> convert(event.getLevel()).ordinal() < logLevelComparable.value.ordinal();
                case LESS_THAN_OR_EQUAL ->
                        event -> convert(event.getLevel()).ordinal() <= logLevelComparable.value.ordinal();
                case NOT_EQUAL -> event -> convert(event.getLevel()).ordinal() != logLevelComparable.value.ordinal();
                case GREATER_THAN -> event -> convert(event.getLevel()).ordinal() > logLevelComparable.value.ordinal();
                case GREATER_THAN_OR_EQUAL ->
                        event -> convert(event.getLevel()).ordinal() >= logLevelComparable.value.ordinal();
                case null -> throw new IllegalStateException("Unexpected value: " + null);
            };
            levelPredicate.add(p);
        });
        return levelPredicate;
    }

    private Predicate<LogEvent> createStringMatchPredicates(List<Config.StringComparable> stringRules, Function<LogEvent, String> logEventStringFunction, boolean reentry) {
        OrPredicate<LogEvent> stringMatchPredicate = new OrPredicate<>();
        stringRules.forEach(stringComparable -> stringMatchPredicate.add(createStringMatchPredicate(stringComparable, logEventStringFunction, reentry)));
        return stringMatchPredicate;
    }

    private Predicate<LogEvent> createStringMatchPredicate(Config.StringComparable stringComparable, Function<LogEvent, String> logEventStringFunction, boolean reentry) {
        Predicate<LogEvent> p = switch (stringComparable.relation) {
            case STARTS_WITH -> event -> logEventStringFunction.apply(event).startsWith(stringComparable.value);
            case STARTS_WITH_IGNORE_CASE ->
                    event -> logEventStringFunction.apply(event).toLowerCase(Locale.ROOT).startsWith(stringComparable.value.toLowerCase(Locale.ROOT));
            case ENDS_WITH -> event -> logEventStringFunction.apply(event).endsWith(stringComparable.value);
            case ENDS_WITH_IGNORE_CASE ->
                    event -> logEventStringFunction.apply(event).toLowerCase(Locale.ROOT).endsWith(stringComparable.value.toLowerCase(Locale.ROOT));
            case CONTAINS -> event -> logEventStringFunction.apply(event).contains(stringComparable.value);
            case CONTAINS_IGNORE_CASE ->
                    event -> logEventStringFunction.apply(event).toLowerCase(Locale.ROOT).contains(stringComparable.value.toLowerCase(Locale.ROOT));
            case MATCH -> event -> logEventStringFunction.apply(event).equals(stringComparable.value);
            case MATCH_IGNORE_CASE ->
                    event -> logEventStringFunction.apply(event).equalsIgnoreCase(stringComparable.value);
            case REGEX ->
                    event -> logEventStringFunction.apply(event).matches(stringComparable.value); // TODO This can be optimized
            case null -> throw new IllegalStateException("Unexpected value: " + null);
        };
        if (!reentry && !stringComparable.whitelist.isEmpty()) {
            p = p.and(createStringMatchPredicates(stringComparable.whitelist, logEventStringFunction, true).negate());
        }
        return p;
    }

    private Predicate<LogEvent> createStringMapMatchPredicate(List<Map<Integer, Config.StringComparable>> stringMapRules, BiFunction<LogEvent, Integer, String> logEventStringFunction) {
        OrPredicate<LogEvent> stringMapMatchPredicate = new OrPredicate<>();
        stringMapRules.forEach(stringMapRule -> {
            AndPredicate<LogEvent> mapMatchPredicate = new AndPredicate<>();
            stringMapRule.forEach((key, value) -> mapMatchPredicate.add(createStringMatchPredicate(value, event -> logEventStringFunction.apply(event, key), false)));
            stringMapMatchPredicate.add(mapMatchPredicate);
        });
        return stringMapMatchPredicate;
    }

    @Override
    public boolean test(LogEvent event) {
        return predicate.test(event);
    }

    private static class AndPredicate<T> implements Predicate<T> {
        private Predicate<T> predicate;

        private void add(Predicate<T> predicate) {
            if (this.predicate == null) {
                this.predicate = predicate;
            } else {
                this.predicate = this.predicate.and(predicate);
            }
        }

        @Override
        public boolean test(T t) {
            if (this.predicate == null) return true;
            return this.predicate.test(t);
        }
    }

    private static class OrPredicate<T> implements Predicate<T> {
        private Predicate<T> predicate;

        private void add(Predicate<T> predicate) {
            if (this.predicate == null) {
                this.predicate = predicate;
            } else {
                this.predicate = this.predicate.or(predicate);
            }
        }

        @Override
        public boolean test(T t) {
            if (this.predicate == null) return true;
            return this.predicate.test(t);
        }
    }
}
