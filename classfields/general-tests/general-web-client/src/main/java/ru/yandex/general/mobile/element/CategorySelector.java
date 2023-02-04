package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CategorySelector extends VertisElement {

    @Name("Кнопка очистки")
    @FindBy(".//div[contains(@class, '_buttonIcon')]")
    VertisElement clearButton();

}
