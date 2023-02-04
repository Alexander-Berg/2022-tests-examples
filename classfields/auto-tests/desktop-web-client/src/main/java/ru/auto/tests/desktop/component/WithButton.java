package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithButton {

    String SEND = "Отправить";
    String CHANGE = "Изменить";
    String SAVE = "Сохранить";

    @Name("Кнопка")
    @FindBy(".//a")
    VertisElement button();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//a[.= '{{ text }}'] | " +
            ".//button[.= '{{ text }}'] | " +
            ".//span[.= '{{ text }}'] | " +
            ".//div[@role='button' and .='{{ text }}']")
    VertisElement button(@Param("text") String text);

    @Name("Кнопка, в названии которой есть «{{ text }}»")
    @FindBy(".//a[contains(., '{{ text }}')] | " +
            ".//span[contains(., '{{ text }}')] | " +
            ".//button[contains(., '{{ text }}')] | " +
            ".//div[@role='button' and contains(., '{{ text }}')]")
    VertisElement buttonContains(@Param("text") String text);
}
