package ru.auto.tests.commons.utils;

import org.junit.runner.Description;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

/**
 * Created by vicdev on 14.08.17.
 */
public class DefaultMarkerManager implements MarkerManager {

    private static final int MAX_LENGTH = 55;
    private static final int RANDOM_PATH_LENGTH = 5;
    private String id;

    /**
     * Возвращаем рандомный индификатор для тестового метода следующего вида: {ClassName}.{MethodName}.{RandomString}
     * @param description
     */
    @Override
    public void generate(Description description) {
        String className = description.getTestClass().getSimpleName();
        String methodName = description.getMethodName();
        StringBuilder builder = new StringBuilder(randomAlphanumeric(RANDOM_PATH_LENGTH));

        if (className.matches("[a-zA-Z0-9]+") && className.length() < (MAX_LENGTH - RANDOM_PATH_LENGTH - 1)) {
            builder.insert(0, className + ".");
        }

        if (methodName.matches("[a-zA-Z0-9]+") && (methodName.length() + builder.toString().length())
                < (MAX_LENGTH - RANDOM_PATH_LENGTH - 1)) {
            builder.insert(className.length() + 1, methodName + ".");
        }
        id = builder.toString();

    }

    @Override
    public String getId() {
        return id;
    }
}
