package ru.yandex.realty.element.popups;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface RefillWalletPopup extends Button {

    @Name("Поле ввода суммы")
    @FindBy(".//input")
    AtlasWebElement input();

    @Name("Крестик закрытия попапа")
    @FindBy(".//button[contains(@class, 'CloseModalButton')]")
    AtlasWebElement closeButton();

    @Name("Кнопка «Выйти»")
    @FindBy(".//div[contains(@class, 'ExitScreen__controls')]/span")
    AtlasWebElement exit();

    @Name("Заголовок попапа")
    @FindBy(".//h2")
    AtlasWebElement h2();
}
