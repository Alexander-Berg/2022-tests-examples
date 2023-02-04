package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;

public interface NewbuildingCardDocuments extends Link {

    @Name("Список документов")
    @FindBy("//a[contains(@class,'NewbuildingCardDocuments__link')]")
    ElementsCollection<AtlasWebElement> docs();
}
