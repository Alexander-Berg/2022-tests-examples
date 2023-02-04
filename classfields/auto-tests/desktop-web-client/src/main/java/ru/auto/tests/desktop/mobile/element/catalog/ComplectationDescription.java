package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ComplectationDescription extends VertisElement {

    @Name("Блок номер «{{ text }}»")
    @FindBy("(.//div[contains(@class, 'features catalog__features')])[{{ text }}]")
    VertisElement block(@Param("text") String linkText);
}