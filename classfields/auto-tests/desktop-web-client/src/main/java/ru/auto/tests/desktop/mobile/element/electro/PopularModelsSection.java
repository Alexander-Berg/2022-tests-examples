package ru.auto.tests.desktop.mobile.element.electro;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PopularModelsSection extends Section {

    @Name("Карусель популярных моделей")
    @FindBy(".//div[contains(@class, '_carouselItemWrapper')]")
    ElementsCollection<VertisElement> models();

}
