package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithButton {

    String SEND = "Отправить";
    String NEXT = "Далее";
    String DELETE_TEXT = "Удалить";
    String FIND = "Найти";

    @Name("Кнопка")
    @FindBy(".//a")
    VertisElement button();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//button[.= '{{ text }}'] | " +
            ".//a[.= '{{ text }}'] | " +
            ".//span[.= '{{ text }}']")
    VertisElement button(@Param("text") String text);

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//a[contains(., '{{ text }}')] | " +
            ".//span[contains(., '{{ text }}')]")
    VertisElement buttonContains(@Param("text") String text);

    @Name("Задизабленная кнопка «{{ text }}»")
    @FindBy(".//button[contains(@class, 'Button_disabled') and .= '{{ text }}']")
    VertisElement buttonDisabled(@Param("text") String text);
}