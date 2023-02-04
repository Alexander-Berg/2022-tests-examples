package ru.auto.tests.desktop.component;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public interface WithInput extends VertisElement {

    @Name("Инпут")
    @FindBy(".//input | " +
            ".//textarea")
    VertisElement input();

    @Name("Инпут «{{ text }}»")
    @FindBy(".//div[contains(@class, 'card__row grid') and .//div[ .='{{ text }}']]//input | " +
            ".//div[contains(@class, 'card__row grid') and .//div[ .='{{ text }}']]//textarea | " +
            ".//input[preceding-sibling::div[. = '{{ text }}']] | " +
            ".//input[@placeholder = '{{ text }}'] | " +
            ".//input[@name = '{{ text }}'] | " +
            ".//textarea[@placeholder = '{{ text }}'] | " +
            ".//textarea[@id = '{{ text }}'] | " +
            ".//textarea[preceding-sibling::div[. = '{{ text }}']] | " +
            ".//div[contains(@class, 'TextInput__input') and .//div[.='{{ text }}']]//input | " +
            ".//div[contains(@class, 'TextArea__box') and .//div[.= '{{ text }}']]//textarea | " +
            ".//div[contains(@class, 'VinHistoryScore__extended') and .//div[.= '{{ text }}']]//textarea | " +
            ".//span[contains(@class, 'input') and .//div[ .='{{ text }}']]//input | " +
            ".//div[contains(@class, '{{ text }}')]//input | " +
            ".//div[contains(@class, 'Card') and contains(@class, '__field') and .//div[.= '{{ text }}']]//input | " +
            ".//div[contains(@class, 'Card') and contains(@class, '__field') and .//div[.= '{{ text }}']]//textarea")
    VertisElement input(@Param("text") String Text);

    @Name("Инпут «{{ text }}» с параметром «{{ сode }}»")
    @FindBy(".//div[contains(@class, 'card__row grid') and " +
            ".//div[ .='{{ text }}']]//input[contains(@name, '{{ code }}')] | " +
            ".//div[contains(@class, '__label') and (.='Телефон')]/.././/input[contains(@name, '{{ code }}')]")
    VertisElement inputPhone(@Param("text") String Text, @Param("code") String Code);

    @Name("Кнопка очистки инпута «{{ text }}»")
    @FindBy(".//div[contains(@class, 'TextInput__input') and .//div[ .='{{ text }}']]" +
            "//i[contains(@class, 'TextInput__clear_visible')]")
    VertisElement clearInputButton(@Param("text") String Text);

    @Name("Инпут для файлов")
    @FindBy(".//input[@type = 'file']")
    VertisElement fileInput();

    @Name("Красное поле «{{ value }}»")
    @FindBy("//label[contains(@class,'TextInput_error')]")
    VertisElement inputError(@Param("value") String value);

    @Step("Инпут «{name}» -> «{value}»")
    default void input(String name, String value) {
        clearInput(name);
        input(name).sendKeys(value);
    }

    @Step("Инпут «{name} -> {value} с таймаутом {timeout}» мс")
    default void input(String name, String value, int timeout) {
        waitSomething(timeout, TimeUnit.MILLISECONDS);
        clearInput(name);
        input(name).sendKeys(value);
    }

    @Step("Очищаем текстовое поле «{name}»")
    default void clearInput(String name) {
        input(name).clear();
        input(name).sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
    }

    @Step("Очищаем текстовое поле")
    default void clearInput() {
        input().clear();
        input().sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
    }

}
