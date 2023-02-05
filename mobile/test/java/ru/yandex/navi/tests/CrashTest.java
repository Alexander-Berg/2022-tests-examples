package ru.yandex.navi.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.Point;
import ru.yandex.navi.tf.RetryRunner;

@RunWith(RetryRunner.class)
@Ignore()
public final class CrashTest extends BaseTest {
    @Test
    public void checkAssert() {
        clickCrashTest("Assert");
    }

    @Test
    public void checkAnr() {
        clickCrashTest("Anr");
        final Point pt = user.getWindowCenter();
        user.swipe(pt.x, pt.y, pt.x, 0);
    }

    private void clickCrashTest(String item) {
        System.err.println(String.format("Click '%s' to crash", item));
        tabBar.clickMenu().clickSettings().click(
            "Developer settings", "Misc", "Crash test", item);
    }
}
