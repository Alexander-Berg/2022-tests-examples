package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface LkPaymentMethodsPage extends BasePage {

    String ADD_BUTTON = "Добавить";
    String SUCCESS_CARD = "5000000000000447";

    @Name("Инпут карты")
    @FindBy(".//input[@id = 'card-number__input']")
    AtlasWebElement cardNumberInput();

    @Name("Инпут срока действия")
    @FindBy(".//input[@id = 'card-expiration__input']")
    AtlasWebElement cardExpiration();

}
