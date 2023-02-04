package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;

public interface VasLanding extends VertisElement, WithButton, WithCheckbox {

    @Name("Пакет «{{ text }}»")
    @FindBy(".//div[contains(@class, 'vas-package ') and .//div[.= '{{ text }}']]")
    VertisElement pack(@Param("text") String text);

    @Name("Опция «{{ text }}»")
    @FindBy(".//div[contains(@class, 'vas-service ') and .//div[.= '{{ text }}']]")
    VertisElement option(@Param("text") String text);

}