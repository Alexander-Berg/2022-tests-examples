package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.saleads.InputField;

public interface FavPopup extends Button, InputField {

    @Name("Кнопка «закрыть»")
    @FindBy(".//button[contains(@class,'NewbuildingSubscriptionModal__closeModalButton')]")
    RealtyElement closeButton();
}
