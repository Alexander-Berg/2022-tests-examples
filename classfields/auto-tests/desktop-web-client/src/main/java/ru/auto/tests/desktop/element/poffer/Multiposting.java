package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Multiposting extends VertisElement {

    @Name("Авто.ру")
    @FindBy(".//label[.//div[contains(@class, 'multiposting__logo_autoru')]]")
    VertisElement autoru();

    @Name("Авито")
    @FindBy(".//label[.//div[contains(@class, 'multiposting__logo_avito')]]")
    VertisElement avito();

    @Name("Дром")
    @FindBy(".//label[.//div[contains(@class, 'multiposting__logo_drom')]]")
    VertisElement drom();
}