package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface ApartmentBlock extends Button {

    @Name("Кнопка раскрытия категории")
    @FindBy(".//span[@class='CardOffersType__count']")
    AtlasWebElement openBlock();
}
