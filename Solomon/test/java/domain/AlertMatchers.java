package ru.yandex.solomon.alert.domain;

import java.util.function.Function;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.mute.domain.Mute;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.util.CommonMatchers;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author Vladimir Gordiychuk
 */
public final class AlertMatchers {
    private AlertMatchers() {
    }

    public static Matcher<Alert> alertIdEqualTo(String alertId) {
        return AlertMatchers.<Alert, String>newBuilder()
                .setMatcher(equalTo(alertId))
                .setValueOf(Alert::getId)
                .setDescription("alert id equal to")
                .setName("alert id")
                .build();
    }

    public static Matcher<Alert> alertTypeEqualTo(AlertType type) {
        return AlertMatchers.<Alert, AlertType>newBuilder()
                .setMatcher(equalTo(type))
                .setValueOf(Alert::getAlertType)
                .setDescription("alert type equal to")
                .setName("alert type")
                .build();
    }

    public static Matcher<Alert> groupKeyEqualTo(Labels groupKey) {
        return AlertMatchers.<Alert, Labels>newBuilder()
                .setMatcher(equalTo(groupKey))
                .setValueOf(alert -> {
                    if (alert instanceof SubAlert) {
                        return ((SubAlert) alert).getGroupKey();
                    }

                    return null;
                })
                .setDescription("alert group key equal to")
                .setName("group key")
                .build();
    }

    public static Matcher<Alert> reflectionEqualTo(Alert expect) {
        return CommonMatchers.reflectionEqualTo(expect);
    }

    public static Matcher<Notification> reflectionEqualTo(Notification expect) {
        return CommonMatchers.reflectionEqualTo(expect);
    }

    public static Matcher<Mute> reflectionEqualTo(Mute expect) {
        return CommonMatchers.reflectionEqualTo(expect);
    }

    private static <T, U> Builder<T, U> newBuilder() {
        return new Builder<>();
    }

    private static class Builder<T, U> {
        private Matcher<? super U> matcher;
        private String description;
        private String name;
        private Function<T, U> valueOf;

        public Builder<T, U> setMatcher(Matcher<? super U> matcher) {
            this.matcher = matcher;
            return this;
        }

        public Builder<T, U> setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder<T, U> setName(String name) {
            this.name = name;
            return this;
        }

        public Builder<T, U> setValueOf(Function<T, U> function) {
            this.valueOf = function;
            return this;
        }

        public FeatureMatcher<T, U> build() {
            return new FeatureMatcher<T, U>(matcher, description, name) {
                @Override
                protected U featureValueOf(T actual) {
                    return valueOf.apply(actual);
                }
            };
        }
    }
}
