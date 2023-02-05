package ru.yandex.navi.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;

@RunWith(RetryRunner.class)
public final class Android5Test extends BaseAndroidTest {
    public Android5Test() {
        userCaps.platformVersion = "5.1";

        // Set location to have intro-screen with GDPR license
        //
        userCaps.initLocation = YANDEX;
    }

    @Test
    public void launch() {
        rotateAndReturn();
    }
}
