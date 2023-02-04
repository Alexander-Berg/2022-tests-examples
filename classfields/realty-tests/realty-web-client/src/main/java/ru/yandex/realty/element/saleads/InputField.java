package ru.yandex.realty.element.saleads;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.RealtyElement;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface InputField extends AtlasWebElement {

    @Name("Поле ввода «{{ value }}»")
    @FindBy(".//input[contains(@placeholder, '{{ value }}')]")
    AtlasWebElement input(@Param("value") String value);

    @Name("Поле ввода c id «{{ value }}»")
    @FindBy(".//input[@id = '{{ value }}']")
    AtlasWebElement inputId(@Param("value") String value);

    // TODO: не универсально пофиксить
    @Name("Крестик очистки поля ввода «{{ value }}»")
    @FindBy("(.//input[contains(@placeholder, '{{ value }}')]/..)//i")
    RealtyElement clearSign(@Param("value") String value);

    @Name("Поле ввода")
    @FindBy(".//input")
    RealtyElement input();

    @Name("Крестик очистки")
    @FindBy(".//i")
    RealtyElement clearSign();

    @Step("Вводим в «{to}» -> «{value}»")
    default void input(String to, String value) {
        input(to).sendKeys(value);
    }

}
