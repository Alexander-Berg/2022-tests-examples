package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithSelect;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 16.04.18
 */
public interface SalesFiltersBlock extends VertisElement, WithButton, WithSelect, WithInput {

    String ALL_PARAMETERS = "Все параметры";
    String MINIMIZE = "Свернуть";

    @Name("Чекбокс груповых операций")
    @FindBy(".//label[contains(@class, 'GroupOperationsCheckbox__checkbox')]")
    VertisElement groupOperationCheckbox();

    @Name("Список фильтров")
    @FindBy(".//div[contains(@class, 'SalesOfferFilters__filter')]")
    ElementsCollection<VertisElement> filtersList();

}
