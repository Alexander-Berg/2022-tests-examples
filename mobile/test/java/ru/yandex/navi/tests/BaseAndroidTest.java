package ru.yandex.navi.tests;

import com.google.common.collect.ImmutableMap;

import org.junit.experimental.categories.Category;

import ru.yandex.navi.categories.SkipIos;
import ru.yandex.navi.tf.AndroidUser;
import ru.yandex.navi.tf.Platform;

@Category(SkipIos.class)
class BaseAndroidTest extends BaseTest {
    AndroidUser androidUser;

    BaseAndroidTest() {
        userCaps.platform = Platform.Android;
    }

    @Override
    void doBeforeStart() {
        androidUser = (AndroidUser) user;
    }

    Object runShellCommand(String command, Object... args) {
        return androidUser.getDriver().executeScript(
                "mobile: shell",
                ImmutableMap.of("command", command, "args", args));
    }
}
