package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface WithInput extends VertisElement {

    @Name("Инпут")
    @FindBy(".//input | " +
            ".//textarea")
    VertisElement input();

    @Name("Инпут «{{ value }}»")
    @FindBy(".//input[contains(@placeholder, '{{ value }}')] | " +
            ".//div[contains(@class, 'TextInput__placeholder') and .= '{{ value }}']/following-sibling::input | " +
            ".//div[contains(@class, 'TextInput__placeholder') and .= '{{ value }}']/following-sibling::div/input | " +
            ".//div[contains(@class, 'TextArea__placeholder') and .= '{{ value }}']/following-sibling::textarea | " +
            ".//div[contains(@class, 'VinCheckInputAmp__placeholder') and .= '{{ value }}']/following-sibling::input")
    VertisElement input(@Param("value") String value);

    @Name("Красное поле «{{ value }}»")
    @FindBy("//span[contains(@class,'error') and contains(@class,'input')]" +
            "//input[contains(@placeholder,'{{ value }}')]")
    VertisElement inputError(@Param("value") String value);

    @Name("Сообщение об ошибке")
    @FindBy(".//span[contains(@class, 'TextInput__error')]")
    VertisElement errorMessage();

    @Name("Инпут для файлов")
    @FindBy(".//input[@type = 'file']")
    VertisElement fileInput();

    @Step("Инпут «{name} -> {value}»")
    default void input(String name, String value) {
        clearInput(name);
        input(name).sendKeys(value);
    }

    @Step("Очищаем текстовое поле «{name}»")
    default void clearInput(String name) {
        input(name).clear();
        input(name).sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
    }
}