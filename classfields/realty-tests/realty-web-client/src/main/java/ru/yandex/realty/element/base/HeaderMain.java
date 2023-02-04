package ru.yandex.realty.element.base;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ButtonWithTitle;
import ru.yandex.realty.element.Link;

public interface HeaderMain extends Link, ButtonWithTitle {

    String NEW_OFFER = "Добавить объявление";
    String LOGIN = "Войти";
    String REFILL_BUTTON = "Пополнить";

    @Name("Кнопка «Выбор региона»")
    @FindBy(".//span[contains(@class, 'RegionSelect')]")
    AtlasWebElement regionSelector();

    @Name("Кнопка «Избранное»")
    @FindBy("//a[contains(@href,'/fav')][.//div[@class='NavBarStuffMenu__favoritesLink']]")
    AtlasWebElement favoritesButton();

    @Name("Иконка аккаунта")
    @FindBy(".//div[@class='User']")
    AtlasWebElement userAccount();
}
