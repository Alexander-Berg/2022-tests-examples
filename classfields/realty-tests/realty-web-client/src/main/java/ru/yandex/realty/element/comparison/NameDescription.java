package ru.yandex.realty.element.comparison;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 28.06.17.
 */
public interface NameDescription extends AtlasWebElement {

    @Name("Список ссылок на офферы")
    @FindBy(".//a[contains(@class, 'Link')]")
    ElementsCollection<AtlasWebElement> offerLinkList();

    @Name("Кнопка для удаления из сравнения")
    @FindBy(".//span[contains(@class, 'ComparisonRowDesc__delete')]")
    ElementsCollection<AtlasWebElement> offerDeleteButton();
}
