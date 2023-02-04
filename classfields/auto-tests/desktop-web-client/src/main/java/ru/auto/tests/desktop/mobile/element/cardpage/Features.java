package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Features extends VertisElement {

    @Name("Характеристика «{{ text }}»")
    @FindBy(".//li[contains(@class, 'CardInfoRow') and contains(., '{{ text }}')]")
    Feature feature(@Param("text") String text);
}