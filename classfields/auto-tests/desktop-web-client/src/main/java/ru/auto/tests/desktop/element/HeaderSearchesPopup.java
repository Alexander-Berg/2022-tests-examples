package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface HeaderSearchesPopup extends VertisElement {

    @Name("Список сохраненных поисков")
    @FindBy(".//div[contains(@class, 'SubscriptionItemDesktop')]")
    ElementsCollection<VertisElement> savedSearchesList();

    @Step("Получаем сохраненный поиск с индексом {i}")
    default VertisElement getSavedSearch(int i) {
        return savedSearchesList().should(hasSize(greaterThan(i))).get(i);
    }
}
