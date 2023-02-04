package ru.auto.tests.desktop.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Select;

public interface SelectGroup extends Select {

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[contains(@class, 'Select__button')][contains(., '{{ value }}')]")
    VertisElement selectButton(@Param("value") String value);
}