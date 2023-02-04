package ru.auto.tests.desktop.element.cabinet.backonsale;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithInputGroup;
import ru.auto.tests.desktop.component.WithSelect;

public interface Filters extends VertisElement, WithSelect, WithInputGroup, WithInput {

    String VIN_INPUT_PLACEHOLDER = "VIN номер или несколько через запятую";
    String ALL_PARAMETERS = "Все параметры";

    @Name("Кнопка «Все параметры» / «Свернуть»")
    @FindBy(".//span[contains(@class, 'BackOnSaleFilters__actionCollapse')]")
    VertisElement expansionButton();

    @Name("Кнопка Сбросить")
    @FindBy(".//a[contains(@class, 'BackOnSaleFilters__linkReset')]")
    VertisElement resetButton();

    @Name("Кнопка «Найти»")
    @FindBy(".//button[.//span[(.= 'Найти') or (.= 'Обновить')]]")
    VertisElement searchButton();
}
