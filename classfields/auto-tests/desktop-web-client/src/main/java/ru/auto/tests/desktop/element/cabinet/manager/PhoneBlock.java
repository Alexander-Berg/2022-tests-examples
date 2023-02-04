package ru.auto.tests.desktop.element.cabinet.manager;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PhoneBlock extends VertisElement {

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[contains(., '{{ value }}')]")
    VertisElement button(@Param("value") String value);
}