package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Stats extends VertisElement {

    @Name("Ссылка «Узнать подробнее»")
    @FindBy(".//a[contains(@class, 'PriceStatsInfo__stats-link')]")
    VertisElement moreInfoUrl();
}