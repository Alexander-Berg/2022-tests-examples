package ru.yandex.realty.mobile.element.subscriptions;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.mobile.element.CloseCross;

public interface SubscriptionPopup extends InputField, Button, CloseCross {

    String SAVE_BUTTON = "Сохранить";
    String ADJUST_SUBSCRIPTION = "Настроить уведомления";
    String DELETE_SUBSCRIPTION = "Удалить подписку";
    String CANCEL_SUBSCRIPTION = "Отмена";
    String IMMEDIATELY = "Сразу";
    String ONCE_AN_HOUR = "Раз в час";
    String ONCE_A_DAY = "Раз в день";
    String ONCE_A_WEEK = "Раз в неделю";

    @Name("Крестик очистки")
    @FindBy("//span[contains(@class,'TextInput__clear_visible')]")
    AtlasWebElement clearCross();
}
