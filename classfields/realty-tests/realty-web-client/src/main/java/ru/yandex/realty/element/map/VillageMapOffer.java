package ru.yandex.realty.element.map;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface VillageMapOffer extends AtlasWebElement {

    @Name("Линк КП")
    @FindBy(".//a[.//div[contains(@class,'Link__click-area')]]")
    AtlasWebElement kpLink();

    @Name("Линк застройщика")
    @FindBy(".//a[contains(@class,'VillageSnippetSearch__developer')]")
    AtlasWebElement developerLink();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[contains(@class,'SnippetContacts__button')]")
    AtlasWebElement showPhoneButton();

    @Name("Добавить в избранное")
    @FindBy(".//div[contains(@class,'VillageSnippetSearch__favorite')]")
    AtlasWebElement addToFavorite();
}
