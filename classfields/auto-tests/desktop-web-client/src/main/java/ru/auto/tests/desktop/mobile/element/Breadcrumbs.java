package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Breadcrumbs extends VertisElement, WithButton {

    @Name("Список элементов в крошках")
    @FindBy(".//div[@class = 'CardBreadcrumbs__item'] | " +
            ".//li[@class = 'VersusBreadcrumbs__item']")
    ElementsCollection<BreadcrumbsItem> itemsList();

    @Step("Получаем элемент хлебных крошек с индексом {i}")
    default BreadcrumbsItem getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}
