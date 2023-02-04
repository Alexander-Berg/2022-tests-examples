package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.cardpage.CallbackPopup;

public interface WithCallbackPopup {

    @Name("Поп-ап обратного звонка")
    @FindBy("//div[contains(@class, 'Modal_visible') and .//div[contains(., 'Заявка на обратный звонок')]]" +
            "//div[contains(@class, 'Modal__content')]")
    CallbackPopup callbackPopup();
}