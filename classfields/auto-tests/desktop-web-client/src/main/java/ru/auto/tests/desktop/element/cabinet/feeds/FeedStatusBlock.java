package ru.auto.tests.desktop.element.cabinet.feeds;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface FeedStatusBlock extends VertisElement {

    @Name("Статус «{{ text }}»")
    @FindBy("//label[contains(@class, 'Radio') and contains(@title, '{{ text }}') ]")
    VertisElement statusLink(@Param("text") String text);

    @Name("Список объявлений")
    @FindBy(".//tr[@class='FeedsDetailsOffersItem__row']")
    ElementsCollection<SaleStatus> salesStatusesList();

    @Step("Получаем загрузку с индексом {i}")
    default SaleStatus getSaleStatus(int i) {
        return salesStatusesList().should(hasSize(greaterThan(i))).get(i);
    }
}


