package ru.yandex.navi.tests;

import io.qameta.allure.Issue;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.ZeroSpeedBanner;

@RunWith(RetryRunner.class)
public class ZsbTest extends BaseTest {
    @Test
    @Category({Light.class, UnstableIos.class})
    @Issue("MOBNAVI-20397")
    @Ignore("MOBNAVI-23917")
    public void showBanner() {
        experiments.enable("navi_feature_zero_speed_ads", "navi_feature_force_zero_speed_state")
            .set("navi_ad_product_cooldown_zero_speed_banner", "1")
            .apply();

        mapScreen.buildRouteBySearchAndGo("Зеленоград");

        restartAppAndSkipIntro();

        ZeroSpeedBanner.getVisible();
        rotateAndReturn();
    }
}
