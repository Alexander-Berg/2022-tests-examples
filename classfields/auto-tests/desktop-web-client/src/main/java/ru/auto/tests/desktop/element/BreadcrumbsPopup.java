package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface BreadcrumbsPopup extends VertisElement, WithButton {

    @Name("Список марок/моделей")
    @FindBy(".//li[contains(@class, 'BreadcrumbsPopupList__popupItem')]/a")
    ElementsCollection<VertisElement> itemsList();

    @Step("Получаем марку/модель с индексом {i}")
    default VertisElement getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}