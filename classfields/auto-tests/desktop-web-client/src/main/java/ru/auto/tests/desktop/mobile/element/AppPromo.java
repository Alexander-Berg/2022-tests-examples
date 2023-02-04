package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AppPromo extends VertisElement {

    @Name("Кнопка установки приложения")
    @FindBy(".//a[contains(@class, '__downloadButton')] | " +
            ".//a[contains(@class, 'FullScreenBanner__button_ok')] | " +
            ".//a[contains(@class, 'Button_color_red')]")
    VertisElement installButton();

    @Name("Кнопка «Пропустить»")
    @FindBy(".//div[contains(@class, '__close')] | " +
            ".//a[contains(@class, 'FullScreenBanner__button_cancel')] |" +
            ".//button[contains(@class, 'BottomAppBanner__button_cancel')] | " +
            ".//button[contains(@class, 'FullScreenSplashCarBanner__button_cancel')] |" +
            ".//div[contains(@class, 'CloseButton')] |" +
            ".//*[contains(@class, 'lose')]")
    VertisElement skipButton();
}