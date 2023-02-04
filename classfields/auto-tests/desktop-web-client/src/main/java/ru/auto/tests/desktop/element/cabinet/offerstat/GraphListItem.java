package ru.auto.tests.desktop.element.cabinet.offerstat;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface GraphListItem extends VertisElement {

    @Name("Поп-ап на графике")
    @FindBy(".//div[contains(@class, 'recharts-tooltip-wrapper')]")
    VertisElement popupPoint();
}
