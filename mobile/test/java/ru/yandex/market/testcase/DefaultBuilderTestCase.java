package ru.yandex.market.testcase;

import org.junit.Test;

import ru.yandex.market.CommonFailMessages;

import static org.junit.Assert.fail;

public abstract class DefaultBuilderTestCase {

    protected abstract void buildDefaultInstance();

    protected abstract Class<?> getObjectClass();

    @Test
    public void testBuildsDefaultInstanceWithoutException() {
        try {
            buildDefaultInstance();
        } catch (Throwable exception) {
            fail(CommonFailMessages.defaultInstance(getObjectClass(), exception));
        }
    }
}
