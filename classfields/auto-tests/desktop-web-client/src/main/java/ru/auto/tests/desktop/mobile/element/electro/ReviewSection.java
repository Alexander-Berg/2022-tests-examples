package ru.auto.tests.desktop.mobile.element.electro;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ReviewSection extends Section {

    String SHOW_MORE_REVIEW = "Показать больше отзывов";

    @Name("Карусель ревью")
    @FindBy(".//div[contains(@class, '_carouselItemWrapper')]")
    ElementsCollection<VertisElement> reviews();

}
