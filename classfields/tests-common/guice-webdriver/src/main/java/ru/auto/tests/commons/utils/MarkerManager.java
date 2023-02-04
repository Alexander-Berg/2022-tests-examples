package ru.auto.tests.commons.utils;

import org.junit.runner.Description;

/**
 * Created by vicdev on 14.08.17.
 */
public interface MarkerManager {

    void generate(Description description);

    String getId();
}
