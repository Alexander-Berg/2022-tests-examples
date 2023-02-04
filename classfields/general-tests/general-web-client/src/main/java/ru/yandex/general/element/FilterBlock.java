package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FilterBlock extends Input, Checkbox, Switcher {

    @Name("Показать все")
    @FindBy(".//div[contains(@class,'SearchableCheckbox__expandButton')]")
    VertisElement showAll();

    @Name("Очистка инпута")
    @FindBy(".//span[contains(@class, 'input-Clear_visible')]")
    VertisElement clearInput();

}
