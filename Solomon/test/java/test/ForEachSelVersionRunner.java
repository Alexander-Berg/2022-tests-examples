package ru.yandex.solomon.expression.test;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.runners.Parameterized;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class ForEachSelVersionRunner extends Parameterized {

    private static Class<?> check(Class<?> clazz) {
        if (!VersionedSelTestBase.class.isAssignableFrom(clazz)) {
            throw new RuntimeException("ForEachSelVersionRunner is suitable only for tests derived from VersionedSelTestBase");
        }
        return clazz;
    }

    public ForEachSelVersionRunner(Class<?> clazz) throws Throwable {
        super(check(clazz));
    }

}
