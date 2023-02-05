package ru.yandex.navi.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.qameta.allure.Issue;
import ru.yandex.navi.categories.PrCheck;
import ru.yandex.navi.tf.RetryRunner;

@RunWith(RetryRunner.class)
public final class AndroidTest extends BaseAndroidTest {
    @Test
    @Category({PrCheck.class})
    public void launch() {
        rotateAndReturn();
    }

    @Test
    @Issue("MOBNAVI-16007")
    public void emptyIntent() {
        androidUser.stopApp();

        runShellCommand("am", "start", "-d", "yandexnavi:");
    }
}
