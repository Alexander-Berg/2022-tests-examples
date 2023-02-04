package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface OutstaffFlatPhotographerPage extends BasePage {

    String SAVE_BUTTON = "Сохранить";

    @Name("Ссылка на фото квартиры")
    @FindBy(".//input[@id = 'OFFER_PHOTO_RAW_URL']")
    AtlasWebElement photoUrl();

    @Name("Ссылка на фото квартиры")
    @FindBy(".//input[@id = 'OFFER_3D_TOUR_URL']")
    AtlasWebElement tour3dUrl();
}
