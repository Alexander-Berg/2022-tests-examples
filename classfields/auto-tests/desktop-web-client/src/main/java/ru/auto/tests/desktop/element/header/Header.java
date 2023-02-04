package ru.auto.tests.desktop.element.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.element.SearchLine;

public interface Header extends VertisElement, WithButton {

    @Name("Кнопка бургера")
    @FindBy(".//div[contains(@class, 'HeaderBurger')]")
    VertisElement burgerButton();

    @Name("Бургер")
    @FindBy("//div[contains(@class, 'HeaderBurger_opened')]//div[contains(@class, 'HeaderBurger__sitemap')]")
    HeaderBurger burger();

    @Name("Лого auto.ru")
    @FindBy(".//a[contains(@class, 'Header__logoLink')]")
    VertisElement logo();

    @Name("Выпадушка зарега")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'HeaderUserMenu__userPopup')]")
    HeaderAuthDropdown authDropdown();

    @Name("Выпадушка кнопки «Продать»")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'HeaderUserMenu__dealerLinksPopup')]")
    HeaderSellButtonDropdown sellButtonDropdown();

    @Name("Аватар")
    @FindBy(".//div[contains(@class, 'HeaderUserMenu__avatar')] | " +
            ".//a[contains(@class, 'HeaderUserMenu__avatar')]")
    VertisElement avatar();

    @Name("Кнопка «Избранное»")
    @FindBy(".//div[contains(@class, 'HeaderMyLink') and .//*[contains(@class, 'IconSvg_favorite')]]")
    VertisElement favoritesButton();

    @Name("Кнопка «Поиски»")
    @FindBy(".//div[contains(@class, 'HeaderMyLink') and .//*[contains(@class, 'IconSvg_subscription')]]")
    VertisElement savedSearchesButton();

    @Name("Кнопка «Сравнения»")
    @FindBy(".//a[contains(@class, 'HeaderMyLink') and .//*[contains(@class, 'IconSvg_car-compare')]]")
    VertisElement compareButton();

    @Name("Кнопка «Сообщения»")
    @FindBy(".//div[contains(@class, 'HeaderMyLink') and .//*[contains(@class, 'IconSvg_chat')]]")
    VertisElement chatButton();

    @Name("Кнопка «Я продаю»")
    @FindBy(".//a[contains(@class, 'HeaderMyLink') and .//*[contains(@class, 'IconSvg_my-offers')]]")
    VertisElement iSellButton();

    @Name("Строка поиска")
    @FindBy(".//div[contains(@class, 'SearchLineSuggest')]")
    SearchLine searchLine();

    @Name("Вторая линия меню")
    @FindBy(".//div[contains(@class, 'Header__secondLine')]")
    HeaderLine2 line2();
}
