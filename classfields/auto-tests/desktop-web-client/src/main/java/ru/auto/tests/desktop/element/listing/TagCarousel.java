package ru.auto.tests.desktop.element.listing;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface TagCarousel extends VertisElement {

    @Name("Список тегов")
    @FindBy(".//li[contains(@class, 'Carousel__item')]/a")
    ElementsCollection<VertisElement> rangesList();

    @Name("Тег «{{ text }}»")
    @FindBy(".//li[contains(@class, 'TagCarousel__item') and ./a[contains(., '{{ text }}')]]")
    VertisElement tag(@Param("text") String text);

}
