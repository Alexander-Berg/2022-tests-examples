package ru.auto.tests.desktop.element.mag;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Sales extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[@class = 'OffersCarousel__title']")
    VertisElement title();

    @Name("Список объявлений")
    @FindBy(".//li[contains(@class, 'CarouselUniversal__item')]")
    ElementsCollection<VertisElement> salesList();

    @Step("Получаем объявление с индексом {i}")
    default VertisElement getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }
}