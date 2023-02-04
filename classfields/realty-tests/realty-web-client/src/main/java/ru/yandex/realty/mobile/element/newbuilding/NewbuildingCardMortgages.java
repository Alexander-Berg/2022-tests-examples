package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.Link;

public interface NewbuildingCardMortgages extends Button, Link {

    String ALL_MORTGAGES = "Все ипотечные программы";

    @Name("Список Ипотек")
    @FindBy(".//div[@class='CardMortgages__item']")
    ElementsCollection<AtlasWebElement> cardMortgages();

    @Name("Зеленая галочка")
    @FindBy(".//div[contains(@class,'CardMortgages__bonus')]")
    AtlasWebElement tick();
}
