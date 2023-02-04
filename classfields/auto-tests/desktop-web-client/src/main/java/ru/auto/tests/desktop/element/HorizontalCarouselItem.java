package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface HorizontalCarouselItem extends VertisElement, WithButton {

    @Name("Название")
    @FindBy(".//a[contains(@class, 'carousel__link')] | " +
            ".//div[contains(@class, 'ListingCarouselItem__name')] | " +
            ".//div[contains(@class, 'CarouselSpecialNewItem__name')] | " +
            ".//p[@class = 'ListingCarouselSameGroup__name'] | " +
            ".//a[contains(@class, 'carousel__title')] | " +
            ".//span[contains(@class, 'SaleCarouselItem__name')]")
    VertisElement title();

    @Name("Фото")
    @FindBy(".//a[contains(@class, 'carousel__thumb')] | " +
            ".//div[contains(@class, 'ListingCarouselItem__thumb')] | " +
            ".//div[contains(@class, 'Brazzers__image')]")
    VertisElement image();

    @Name("Информация")
    @FindBy(".//div[@class = 'ListingCarouselItem__info']")
    VertisElement info();

    @Name("Цена")
    @FindBy(".//div[@class = 'ListingCarouselItem__price']")
    VertisElement price();

}
