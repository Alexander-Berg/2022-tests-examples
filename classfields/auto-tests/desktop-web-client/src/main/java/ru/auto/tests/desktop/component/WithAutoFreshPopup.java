package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.AutoFreshPopup;

public interface WithAutoFreshPopup {

    @Name("Поп-ап автоподнятия")
    @FindBy("//div[@class = 'AutorenewModal ']")
    AutoFreshPopup autoFreshPopup();
}
