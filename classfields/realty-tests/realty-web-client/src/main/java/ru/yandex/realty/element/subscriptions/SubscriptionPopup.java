package ru.yandex.realty.element.subscriptions;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;

public interface SubscriptionPopup extends Button, Link {

    @Name("Селектор частоты расслыки")
    @FindBy(".//button[contains(@class, 'Select__button')]")
    AtlasWebElement selectButton();

    @Name("Получать уведомления {{ value }}")
    @FindBy("//div[contains(@class,'Popup_visible')]//div[contains(@class,'Menu__item')][contains(.,'{{ value }}')]")
    AtlasWebElement notificationItem(@Param("value") String value);

    @Name("Поле ввода email")
    @FindBy(".//span[contains(@class, 'SubscriptionSettingsModal__emailInput')]//input")
    AtlasWebElement emailInput();

    @Name("Кнопка удаления email")
    @FindBy(".//span[contains(@class, 'SubscriptionSettingsModal__emailInput')]//span[contains(@class, 'TextInput__clear_visible')]")
    RealtyElement clearEmailInput();

    @Name("Крестик закрыть")
    @FindBy(".//i[contains(@class,'Icon_type_cross')]")
    AtlasWebElement closeCross();
}
