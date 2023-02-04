package ru.yandex.realty.element.map;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.ButtonWithTitle;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.saleads.InputField;

/**
 * @author kantemirov
 */
public interface MapOffer extends Button, Link, ButtonWithTitle, InputField {

    @Name("Сохранить заметку")
    @FindBy(".//button[contains(@class,'OffersSerpItem__note-save')]")
    AtlasWebElement noteSaveButton();

    @Name("Стрелочка возле цены")
    @FindBy("//i[contains(@class,'PriceTrendIndicator')]")
    RealtyElement arrowPrice();

    @Name("Удалить заметку")
    @FindBy(".//button[contains(@class,'OffersSerpItem__note-remove')]")
    AtlasWebElement noteRemoveButton();

    @Name("Убрать из сравнения")
    @FindBy(".//button[contains(@class,'MapOfferSerpItem__compare_active')]")
    AtlasWebElement removeFromComparison();
}
