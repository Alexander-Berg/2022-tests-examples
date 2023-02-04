package ru.yandex.realty.mobile.element.village;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface FromDevVillage extends Button {

    @Name("Добавить в избранное")
    @FindBy(".//div[contains(@class,'VillagesSerpItem__favor')]")
    AtlasWebElement favButton();

    @Name("Ссылка коттеджного поселка от застройщика")
    @FindBy(".//a[contains(@class,'VillagesSerpItem__link')]")
    AtlasWebElement villageLink();
}
