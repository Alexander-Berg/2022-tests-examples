package ru.yandex.realty.element.ipoteka;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.saleads.ActionBar;
import ru.yandex.realty.element.saleads.InputField;

public interface MortgageOffer extends ActionBar, InputField {

    @Name("Ссылка оффера")
    @FindBy(".//a[contains(@class,'OffersSerpItem') and not(contains(@class,'OffersSerpItem__left'))]")
    AtlasWebElement offerLink();

    @Name("Цена оффера")
    @FindBy(".//div[contains(@class, 'OfferPreviewSnippet__price')]")
    AtlasWebElement price();

    @Name("Добавить в избранное")
    @FindBy(".//div[contains(@class,'OfferPreviewSnippet__favorite')]")
    AtlasWebElement addToFavorite();

    @Name("Кнопка сохранить заметку")
    @FindBy(".//button[contains(@class,'OffersSerpItem__note-save')]")
    AtlasWebElement noteSaveButton();

    @Name("Кнопка удалить заметку")
    @FindBy(".//button[contains(@class,'OffersSerpItem__note-remove')]")
    AtlasWebElement noteRemoveButton();
}
