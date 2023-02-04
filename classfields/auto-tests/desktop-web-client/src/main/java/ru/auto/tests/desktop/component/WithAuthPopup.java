package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.AuthPopup;
import ru.auto.tests.desktop.element.AuthPopupFrame;

public interface WithAuthPopup {

    @Name("Поп-ап авторизации")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'AuthModal')]")
    AuthPopup authPopup();

    @Name("Фрейм поп-апа авторизации")
    @FindBy(".//html")
    AuthPopupFrame authPopupFrame();
}