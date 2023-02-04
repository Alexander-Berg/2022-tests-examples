package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 16.08.17.
 */
public interface SubLocationFilter extends AtlasWebElement {

    @Name("Список подказок")
    @FindBy("//ul[@class = 'popup__items input__popup-items']/li")
    ElementsCollection<AtlasWebElement> suggestionList();

    @Name("Кнопка закрытия попапа локации")
    @FindBy("//button[contains(@class, 'refinement__close')]")
    AtlasWebElement closeButton();

    @Name("Поле ввода")
    @FindBy("//span[contains(@class, 'refinement__suggest')]//input")
    AtlasWebElement input();
}
