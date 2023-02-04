package ru.auto.tests.desktop.mobile.element.compare;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Related extends VertisElement {

    @Name("Список похожих")
    @FindBy(".//div[contains(@class, 'VersusRelatedOffers__item')]")
    ElementsCollection<VertisElement> relatedList();

    @Step("Получаем похожее с индексом {i}")
    default VertisElement getRelated(int i) {
        return relatedList().should(hasSize(greaterThan(i))).get(i);
    }
}
