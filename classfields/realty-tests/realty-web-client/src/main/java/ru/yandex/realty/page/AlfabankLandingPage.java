package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface AlfabankLandingPage extends BasePage {

    String TAKE_DISCONT = "Получить скидку";

    @Name("Калькулятор")
    @FindBy(".//div[contains(@class,'AlfabankPromo__mortgage')]")
    Button calculator();
}
