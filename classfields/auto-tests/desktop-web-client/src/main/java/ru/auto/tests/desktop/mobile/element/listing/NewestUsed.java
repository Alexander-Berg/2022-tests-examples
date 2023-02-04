package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface NewestUsed extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'ListingCarouselNewestUsed__title')]")
    VertisElement title();

    @Name("Список объявлений")
    @FindBy(".//li[@class = 'CarouselUniversal__item']")
    ElementsCollection<VertisElement> itemsList();

    @Step("Получаем объявление с индексом {i}")
    default VertisElement getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }

}
