package ru.yandex.navi.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;

@RunWith(RetryRunner.class)
public final class Android6Test extends BaseAndroidTest {
    public Android6Test() {
        userCaps.platformVersion = "6.0";
    }

    @Test
    public void launch() {
        rotateAndReturn();
        restartAppAndSkipIntro();
        buildRouteToSomePointAndGo();
    }
}
