package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import java.time.Duration;

public final class ZeroSpeedBanner extends BaseScreen {
    @AndroidFindBy(id = "view_card_root")
    @iOSXCUITFindBy(accessibility = "ZeroSpeedCardView")
    public MobileElement view;

    private ZeroSpeedBanner() {
        super();
        setView(view);
    }

    public static ZeroSpeedBanner getVisible() {
        ZeroSpeedBanner banner = new ZeroSpeedBanner();
        banner.checkVisible(Duration.ofSeconds(30));
        return banner;
    }
}
