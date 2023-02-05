package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;

public final class MapPromoBanner extends BaseScreen {
    @AndroidFindBy(id = "image_promobanner_close")
    private MobileElement closeButton;

    private MapPromoBanner() {
        super();
        setView(closeButton);
    }

    public static void dismissIfVisible() {
        MapPromoBanner promoBanner = new MapPromoBanner();
        if (promoBanner.isDisplayed())
            promoBanner.closeBanner();
    }

    @Step("Закрыть map promo banner")
    private void closeBanner() {
        user.clicks(closeButton);
    }
}
