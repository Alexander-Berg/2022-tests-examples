package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

public interface SearchLine extends VertisElement, WithInput {

    @Name("Кнопка очистки ввода")
    @FindBy(".//i")
    VertisElement clearTextButton();

    @Name("Выпадушка с вариантами поиска")
    @FindBy(".//div[contains(@class, 'SearchLineSuggestMobile__content')]")
    SearchLineSuggest suggest();
}
