package ru.yandex.disk.test;

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.mockito.ArgumentMatcher;

import javax.annotation.NonnullByDefault;

@NonnullByDefault
public abstract class AbstractEquals<T> implements ArgumentMatcher<T> {

    private final T wanted;

    public AbstractEquals(T wanted) {
        this.wanted = wanted;
    }

    @SuppressWarnings("unchecked")
    public boolean matches(Object actual) {
        if (actual == null && this.wanted == null) {
            return true;
        } else if (actual == null) {
            return false;
        } else if (wanted == null) {
            return false;
        } else if (actual.getClass().equals(wanted.getClass())) {
            return equals(this.wanted, (T) actual);
        } else {
            return false;
        }
    }

    protected abstract boolean equals(T wanted, T actual);

    public void describeTo(Description description) {
        description.appendText(describe(wanted));
    }

    public String describe(Object object) {
        String text = quoting();
        text += "" + object;
        text += quoting();
        return text;
    }

    private String quoting() {
        if (wanted instanceof String) {
            return "\"";
        } else if (wanted instanceof Character) {
            return "'";
        } else {
            return "";
        }
    }

    protected final Object getWanted() {
        return wanted;
    }

    public SelfDescribing withExtraTypeInfo() {
        return new SelfDescribing() {
            public void describeTo(Description description) {
                String message = describe("(" + wanted.getClass().getSimpleName() + ") " + wanted);
                description.appendText(message);
            }
        };
    }

    public boolean typeMatches(Object object) {
        return wanted != null && object != null && object.getClass() == wanted.getClass();
    }
}