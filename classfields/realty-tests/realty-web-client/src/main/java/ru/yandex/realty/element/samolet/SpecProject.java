package ru.yandex.realty.element.samolet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;

public interface SpecProject extends AtlasWebElement {

    @Name("Список новостроек запина спецпроекта")
    @FindBy(".//div[@data-test='SiteSnippetSearch']")
    ElementsCollection<Link> snippetElements();
}
