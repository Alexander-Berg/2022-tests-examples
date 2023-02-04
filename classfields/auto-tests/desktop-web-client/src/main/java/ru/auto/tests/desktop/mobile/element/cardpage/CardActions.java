package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CardActions extends VertisElement {

    @Name("Кнопка «Сделка»")
    @FindBy(".//div[@class = 'CardActions__button' and .//*[contains(@class, 'IconSvg_safedeal')]]")
    VertisElement dealButton();

    @Name("Кнопка «Поделиться»")
    @FindBy(".//div[@class = 'CardActions__button' and .//*[contains(@class, 'IconSvg_share')]]")
    VertisElement shareButton();

    @Name("Кнопка «Заметка»")
    @FindBy(".//div[@class = 'CardActions__button' and .//*[contains(@class, 'IconSvg_note')]]")
    VertisElement noteButton();

    @Name("Поп-ап заметки")
    @FindBy("//div[contains(@class, 'OfferNoteEditModal') and contains(@class, 'Modal_visible')]" +
            "//div[@class = 'Modal__content']")
    NotePopup notePopup();

    @Name("Кнопка «Добавить в избранное»")
    @FindBy(".//button[.//*[contains(@class, 'IconSvg_favorite Icon')]] | " +
            ".//a[.//*[contains(@class, 'IconSvg_favorite Icon')]]")
    VertisElement addToFavoritesButton();

    @Name("Кнопка «Удалить из избранного»")
    @FindBy(".//button[.//*[contains(@class, 'IconSvg_favorite-active')]]")
    VertisElement deleteFromFavoritesButton();

}
