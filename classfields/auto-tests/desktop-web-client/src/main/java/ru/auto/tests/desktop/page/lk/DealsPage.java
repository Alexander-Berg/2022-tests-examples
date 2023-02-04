package ru.auto.tests.desktop.page.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.lk.DealsListItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DealsPage extends VertisElement, BasePage {

    String CANCEL_REQUEST = "Отменить запрос";
    String SEND_REQUEST = "Отправить запрос";
    String RESUME_DEAL = "Возобновить сделку";
    String GO_TO_DEAL = "Перейти к сделке";
    String GO_TO_OFFER = "Перейти к объявлению";
    String ABOUT_DEAL = "Подробнее о сделке";

    @Name("Список сделок")
    @FindBy("//div[@class='SafeDealListItem']")
    ElementsCollection<DealsListItem> dealsList();

    @Step("Получаем сделку с индексом {i}")
    default DealsListItem getDeal(int i) {
        return dealsList().should(hasSize(greaterThan(i))).get(i);
    }
}
