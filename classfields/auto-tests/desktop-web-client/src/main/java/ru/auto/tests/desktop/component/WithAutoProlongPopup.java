package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.AutoProlongPopup;

public interface WithAutoProlongPopup {

    @Name("Поп-ап автоподнятия")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    AutoProlongPopup openPopup();
}
