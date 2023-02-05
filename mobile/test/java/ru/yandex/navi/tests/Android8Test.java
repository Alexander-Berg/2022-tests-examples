package ru.yandex.navi.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;

@RunWith(RetryRunner.class)
public final class Android8Test extends BaseAndroidTest {
    public Android8Test() {
        userCaps.platformVersion = "8.0";
    }

    @Test
    public void launch() {
        rotateAndReturn();
        restartAppAndSkipIntro();
        buildRouteToSomePointAndGo();
    }
}
