package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Filters extends VertisElement {

    @Name("Фильтры в свернутом состоянии")
    @FindBy(".//button[contains(@class, 'FilterButton_minimized')]")
    VertisElement filtersMinimized();

    @Name("Кнопка перехода к выбору марки")
    @FindBy(".//a[contains(@href, '/marks/')]")
    VertisElement selectMarkButton();

    @Name("Кнопка перехода к выбору модели")
    @FindBy(".//a[contains(@href, '/models/')]")
    VertisElement selectModelButton();

    @Name("Селект поколений")
    @FindBy(".//div[contains(@class, 'section_toggable')]")
    GenerationsSelect generationSelect();
}
