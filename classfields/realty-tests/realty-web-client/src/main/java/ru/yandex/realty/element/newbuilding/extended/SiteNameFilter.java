package ru.yandex.realty.element.newbuilding.extended;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by vicdev on 17.04.17.
 */
public interface SiteNameFilter extends AtlasWebElement {

    @Name("Саджест")
    @FindBy("//ul[contains(@class, 'Suggest__list')]//li")
    ElementsCollection<AtlasWebElement> suggestList();

}
