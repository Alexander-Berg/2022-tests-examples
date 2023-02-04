package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface NewbuildingCardProposals extends Button {

    @Name("Список акций")
    @FindBy("//li[@class='NewbuildingCardProposals__list-item']")
    ElementsCollection<AtlasWebElement> promosList();
}
