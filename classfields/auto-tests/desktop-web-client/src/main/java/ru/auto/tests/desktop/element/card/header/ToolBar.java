package ru.auto.tests.desktop.element.card.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface ToolBar extends VertisElement {

    @Name("Кнопка добавления заметки")
    @FindBy(".//button[./span[contains(@class, 'icon_type_note')]] | " +
            ".//div[contains(@class, 'ButtonNote')]")
    VertisElement noteButton();

    @Name("Кнопка добавления в избранное")
    @FindBy(".//*[contains(@class, 'card__favorite-action') or contains(@class, 'favorite_action_add')] | " +
            ".//div[contains(@class, 'ButtonFavorite')]")
    VertisElement favoriteButton();

    @Name("Кнопка удаления из избранного")
    @FindBy(".//*[contains(@class,'favorite_action_delete')] | " +
            ".//div[contains(@class, 'ButtonFavorite-module__active')] |" +
            ".//div[contains(@class, 'ButtonFavorite_active')]")
    VertisElement favoriteDeleteButton();

    @Name("Кнопка добавления в сравнение")
    @FindBy(".//div[contains(@class, 'compare_off')] | " +
            ".//div[contains(@class, 'ButtonCompare')]")
    VertisElement compareButton();

    @Name("Кнопка удаления из сравнения")
    @FindBy(".//div[contains(@class, 'compare_on')] | " +
            ".//div[contains(@class, 'ButtonCompare_active')]")
    VertisElement compareDeleteButton();

    @Name("Кнопка «Пожаловаться на объявление»")
    @FindBy(".//a[contains(@class,'complain__button')] |" +
            ".//div[contains(@class, 'ButtonReport-module__container')] |" +
            ".//div[contains(@class, 'ButtonReport__content')]")
    VertisElement complainButton();

    @Name("Кнопка успешной отправки жалобы")
    @FindBy(".//a[contains(@class,'complain__button_success')] | " +
            ".//button[contains(@class, 'ButtonReport-module__container')]//*[contains(@class, 'IconSvg_done-old')] |" +
            ".//button[contains(@class, 'ButtonReport')]//*[contains(@class, 'IconSvg_done')]")
    VertisElement complainButtonSuccess();

    @Name("Кнопка скрытия объявления")
    @FindBy(".//button[contains(@class, 'item__close') or contains(@class, 'rollup')] |" +
            ".//*[contains(@class, 'IconSvg_hide ')]")
    VertisElement hideButton();

    @Name("Кнопка показа объявления")
    @FindBy(".//span[contains(@class, 'ListingItemActions__tooltip')] | " +
            ".//button[contains(@class, 'ListingItemActions__rollup')]")
    VertisElement showButton();
}
