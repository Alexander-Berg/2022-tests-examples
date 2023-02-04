package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Sales extends VertisElement {

    @Name("Список объявлений")
    @FindBy(".//li[contains(@class, 'CarouselUniversal__item')]")
    ElementsCollection<VertisElement> salesList();

    @Name("Кнопка «Все»")
    @FindBy(".//a[contains(@class, 'SaleCarousel__footerLink')]")
    VertisElement showAllButton();

    @Step("Получаем объявление с индексом {i}")
    default VertisElement getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }
}