package ru.yandex.infra.stage.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import ru.yandex.infra.stage.deployunit.Readiness;
import ru.yandex.infra.stage.dto.Condition;

public class CustomMatchers {
    public static Matcher<Condition> isTrue() {
        return new TypeSafeDiagnosingMatcher<>() {
            protected boolean matchesSafely(Condition item, Description mismatchDescription) {
                if (item.isTrue()) {
                    return true;
                }
                mismatchDescription.appendText(item.toString());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Condition with TRUE status");
            }
        };
    }

    public static Matcher<Readiness> isReady() {
        return new TypeSafeDiagnosingMatcher<>() {
            protected boolean matchesSafely(Readiness item, Description mismatchDescription) {
                if (item.isReady()) {
                    return true;
                }
                mismatchDescription.appendText(item.toString());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Ready readiness status");
            }
        };
    }
}
