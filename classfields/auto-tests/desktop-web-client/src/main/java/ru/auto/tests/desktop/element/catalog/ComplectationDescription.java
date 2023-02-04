package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ComplectationDescription extends VertisElement {

    @Name("Колонка номер «{{ text }}»")
    @FindBy("(.//div[contains(@class, 'catalog__column catalog__column_half')])[{{ text }}]")
    VertisElement column(@Param("text") String linkText);
}