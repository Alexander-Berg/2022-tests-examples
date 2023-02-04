package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.lk.DealsListItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface LkDealPage extends VertisElement, BasePage {

    @Name("Список сделок")
    @FindBy("//div[@class = 'SafeDealListItem']")
    ElementsCollection<DealsListItem> dealsList();

    @Step("Получаем сделку с индексом {i}")
    default DealsListItem getDeal(int i) {
        return dealsList().should(hasSize(greaterThan(i))).get(i);
    }

}
