package ru.yandex.realty.mobile.element.listing;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.Link;

public interface TouchSite extends Link, Button {

    String CALL = "Позвонить";

    @Name("Добавление в избранное")
    @FindBy(".//div[contains(@class, 'SerpFavoriteAction')]")
    AtlasWebElement favorite();

    @Name("Состояние»")
    @FindBy(".//div[contains(@class, 'state')]")
    AtlasWebElement state();

    @Name("Описание")
    @FindBy(".//div[contains(@class, 'description')]")
    AtlasWebElement description();

    @Name("Тайтл")
    @FindBy(".//div[contains(@class, 'title')]")
    AtlasWebElement title();

    @Name("Позвонить")
    @FindBy(".//button[@data-test='PhoneButton']")
    AtlasWebElement call();

}
