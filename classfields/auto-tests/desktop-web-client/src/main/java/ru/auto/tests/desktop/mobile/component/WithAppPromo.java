package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.AppPromo;

public interface WithAppPromo {

    @Name("Промо приложения")
    @FindBy("//div[contains(@class, 'promo-header ') and not(contains(@class, 'promo-header_hidden'))] | " +
            "//div[contains(@class, 'PromoHeader')] | " +
            "//div[contains(@class, 'FullScreenBanner')] | " +
            "//div[contains(@class, 'FullScreenSplashPhoneBanner')] | " +
            "//div[contains(@class, 'FullScreenSplashCarBanner')] | " +
            "//div[contains(@class, 'BottomAppBanner')] | " +
            "//div[contains(@class, 'FullScreenGarageBanner')] | " +
            "//div[contains(@class, 'ReturningSplash')]")
    AppPromo appPromo();
}