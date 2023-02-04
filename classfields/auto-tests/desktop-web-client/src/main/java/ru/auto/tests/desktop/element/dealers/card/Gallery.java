package ru.auto.tests.desktop.element.dealers.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Gallery extends VertisElement {

    @Name("Карта")
    @FindBy(".//li[contains(@class, 'SalonHeader__carouselItem')]")
    VertisElement map();

    @Name("Список элементов")
    @FindBy(".//li[contains(@class, 'CarouselUniversal__item')]")
    ElementsCollection<GalleryItem> itemsList();

    @Step("Получаем цвет с индексом {i}")
    default GalleryItem getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Стрелка >")
    @FindBy(".//div[contains(@class, 'NavigationButton_next')]")
    VertisElement nextButton();
}