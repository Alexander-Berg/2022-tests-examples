package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Sidebar extends VertisElement {

    @Name("Блок промокодов")
    @FindBy(".//div[contains(@class, 'PromoFeatures')]")
    PromocodeBlock promocodeBlock();

    @Name("Блок «Госуслуги»")
    @FindBy(".//div[contains(@class, 'GosUslugiBenefit')]")
    VertisElement gosuslugiBlock();
}
