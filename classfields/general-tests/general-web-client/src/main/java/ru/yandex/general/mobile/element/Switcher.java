package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Switcher extends VertisElement {

    @Name("Свитчер")
    @FindBy(".//div[contains(@class,'Switcher__switcherWrapper')]")
    VertisElement switcher();
}
