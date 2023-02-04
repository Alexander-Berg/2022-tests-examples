package ru.auto.tests.desktop.element.catalog.card;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ModelSummary extends VertisElement {

    @Name("Галерея")
    @FindBy(".//div[contains(@class, 'catalog-generation-summary__gallery')]")
    VertisElement gallery();

    @Name("Список фото")
    @FindBy(".//a[contains(@class, 'catalog-generation-summary__gen-button')]")
    ElementsCollection<VertisElement> photosList();

}
