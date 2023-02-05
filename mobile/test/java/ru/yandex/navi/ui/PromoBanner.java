package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;

public final class PromoBanner extends BaseScreen {
    @AndroidFindBy(id = "promo_banner_container")
    @iOSXCUITFindBy(accessibility = "Привет, я Алиса")
    private MobileElement view;

    @AndroidFindBy(id = "button_promobanner_negative")
    @iOSXCUITFindBy(accessibility = "Закрыть")
    private MobileElement negativeButton;

    private PromoBanner() {
        super();
        setView(view);
    }

    public static void dismissIfVisible() {
        PromoBanner promoBanner = new PromoBanner();
        if (promoBanner.isDisplayed())
            promoBanner.closeBanner();
    }

    @Step("Закрыть рекламный баннер")
    private void closeBanner() {
        user.clicks(negativeButton);
    }
}
