package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FilterBlock extends Input, Checkbox, Switcher {

    @Name("Показать все")
    @FindBy(".//button[contains(@class,'OfferFilterFormCheckboxSet__button')]")
    VertisElement showAll();

    @Name("Очистка инпута")
    @FindBy(".//span[contains(@class, 'input-Clear_visible')]")
    VertisElement clearInput();

}
