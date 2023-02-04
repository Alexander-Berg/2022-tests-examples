package ru.yandex.infra.stage.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.bolts.collection.Try;

public class IsSuccess<T> extends TypeSafeMatcher<Try<? extends T>> {

    @Override
    public void describeTo(Description description) {
        description.appendText("success try object");
    }

    @Override
    protected boolean matchesSafely(Try<? extends T> item) {
        return item.isSuccess();
    }

    public static <T> Matcher<Try<? extends T>> isSuccess() {
        return new IsSuccess<>();
    }
}
