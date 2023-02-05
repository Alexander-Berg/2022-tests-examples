package ru.yandex.jams.util;

import ru.yandex.misc.property.load.PropertiesLoader;

/**
 * @author Sergey Shinderuk
 * @author Sergey Polovko
 */
public class TestUtils {

    public static void initializeProperties(final String appName) {
        PropertiesLoader.initialize((t, environmentType) -> {
            t.set("appName", appName);

            t.include("classpath:${appName}-default.properties");
            t.includeIfExists("classpath:${appName}-development.properties");
            t.includeIfExists("classpath:${appName}-tests.properties");

            t.dump();
        });
    }

}
