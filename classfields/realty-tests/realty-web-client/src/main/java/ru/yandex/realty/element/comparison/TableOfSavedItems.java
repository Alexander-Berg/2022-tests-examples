package ru.yandex.realty.element.comparison;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 28.06.17.
 */
public interface TableOfSavedItems extends AtlasWebElement {

    @Name("Описание названия оффера")
    @FindBy("//tr[.//h1[.='Сравнение']]")
    NameDescription nameDescription();

    @Name("Список контактов офферов")
    @FindBy(".//tr[3]/td/div")
    ElementsCollection<Contact> contactsList();

    @Name("Список основных фильтров")
    @FindBy(".//tbody[contains(@class, 'main')]/tr[not(contains(@class, 'separator'))]")
    ElementsCollection<AtlasWebElement> mainRowList();

    @Name("Список побочных фильтров")
    @FindBy(".//tbody[contains(@class, 'comparison__section') and not(contains(@class, 'main'))]/tr")
    ElementsCollection<AtlasWebElement> secondaryRowList();

}
