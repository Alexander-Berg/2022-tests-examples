package ru.yandex.realty.element.map;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface MapNewBuildingCard extends Button {

    @Name("Линк ЖК")
    @FindBy(".//a[.//div[contains(@class,'Link__click-area')]]")
    AtlasWebElement jkLink();

    @Name("Линк застройщика")
    @FindBy(".//div[contains(@class,'SiteSnippetSearch__developerBlock')]//a")
    AtlasWebElement developerLink();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[@data-test = 'SnippetContactsPhoneButton']")
    AtlasWebElement showPhoneButton();

    @Name("Добавить в избраннок")
    @FindBy(".//div[contains(@class,'SiteSnippetSearch__favorite')]")
    AtlasWebElement addToFavorite();
}
