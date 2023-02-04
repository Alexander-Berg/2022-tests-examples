package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface Filter extends VertisElement, WithButton {

    @Name("Кнопка перехода к выбору марки")
    @FindBy(".//a[contains(@href, '/marks/')]")
    VertisElement selectMarkButton();

    @Name("Кнопка перехода к выбору модели")
    @FindBy(".//a[contains(@href, '/models/')]")
    VertisElement selectModelButton();

    @Name("Кнопка «Выбрать марку или модель»")
    @FindBy(".//button[contains(@class, 'FilterButton_minimized')]")
    VertisElement selectMarkModelButton();

    @Name("Селект {{ text }}")
    @FindBy(".//div[contains(@class, 'Select') and .//span[.= '{{ text }}']] | " +
            ".//div[@class = 'Select__nativeWrapper' and ./div[.= '{{ text }}']]/select | " +
            ".//select[@class = 'CatalogFilters__select' and ./option[.= '{{ text }}']]")
    VertisElement select(@Param("text") String value);

    @Name("Кнопка «Все параметры»")
    @FindBy(".//a[.//span[text()='Все параметры']]")
    VertisElement allParamsButton();
}
