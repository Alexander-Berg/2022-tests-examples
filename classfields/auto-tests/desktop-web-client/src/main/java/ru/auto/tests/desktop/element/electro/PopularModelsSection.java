package ru.auto.tests.desktop.element.electro;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PopularModelsSection extends Section {

    @Name("Список популярных моделей")
    @FindBy(".//a[contains(@class, 'PopularModelItem')]")
    ElementsCollection<VertisElement> models();

}
