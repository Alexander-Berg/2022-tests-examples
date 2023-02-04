package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.CallbackPopup;

public interface WithCallbackPopup {

    @Name("Поп-ап обратного звонка")
    @FindBy("//div[contains(@class, 'CardCallbackButton__popup')] | " +
            "//div[contains(@class, 'CardCallbackButton__modal')]")
    CallbackPopup callbackPopup();
}
