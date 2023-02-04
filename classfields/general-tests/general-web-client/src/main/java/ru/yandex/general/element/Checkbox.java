package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Checkbox extends VertisElement {

    String CHECKED = "checked";
    String TRUE = "true";

    @Name("Чекбокс «{{ value }}»")
    @FindBy(".//label[contains(., '{{ value }}')][./input[@type = 'checkbox']]")
    Tag checkboxWithLabel(@Param("value") String value);

    @Name("Чекбокс")
    @FindBy(".//label[contains(., '')][./input[@type = 'checkbox']]")
    VertisElement checkboxEmptyLabel();

    @Name("Чекбокс")
    @FindBy(".//input[@type = 'checkbox']")
    VertisElement checkbox();

    @Name("Список активных чекбоксов")
    @FindBy(".//input[@type = 'checkbox'][@checked]")
    ElementsCollection<VertisElement> checkedChecboxList();

}
