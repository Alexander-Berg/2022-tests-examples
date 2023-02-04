package ru.yandex.realty.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface HideableBlock extends PhoneBlock {

    @Name("Добавить к сравнению")
    @FindBy(".//button[.//*[contains(@class,'IconSvg_favorite')]]")
    AtlasWebElement addToFavButton();

}
