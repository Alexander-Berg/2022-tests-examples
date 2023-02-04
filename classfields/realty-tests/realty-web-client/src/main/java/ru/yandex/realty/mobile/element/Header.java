package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface Header extends AtlasWebElement, Link {

    String YANDEX = "Яндекс";
    String REALTY = "Недвижимость";

    @Name("Аватар юзера")
    @FindBy("//div[@class = 'UserAccount']//img")
    AtlasWebElement userAvatar();

}
