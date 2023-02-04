package ru.auto.tests.desktop.element.electro;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface OffersSection extends Section {

    @Name("Список офферов в продаже")
    @FindBy(".//div[contains(@class, '_offer')]")
    ElementsCollection<VertisElement> offers();

}
