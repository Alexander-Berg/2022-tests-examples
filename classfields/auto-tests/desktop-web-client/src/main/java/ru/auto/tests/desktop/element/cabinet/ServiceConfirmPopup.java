package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ServiceConfirmPopup extends VertisElement {

    @Name("Текст")
    @FindBy(".//div[contains(@class, 'Confirm-module__text')]")
    VertisElement text();

    @Name("Кнопка  «{{ text }}»")
    @FindBy(".//button[. = '{{ text }}']")
    VertisElement button(@Param("text") String name);
}