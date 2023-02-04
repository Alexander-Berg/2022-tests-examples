package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.HorizontalCarousel;

public interface BookingStatus extends VertisElement {

    @Name("Статус")
    @FindBy("//div[contains(@class, '__textContent')]")
    VertisElement status();

    @Name("Похожие объявления горизонтальным списком")
    @FindBy("//div[contains(@class, 'CarouselUniversal_dir_horizontal')]")
    HorizontalCarousel horizontalRelated();
}