package ru.auto.tests.desktop.element.cabinet.walkin;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DistributionInfoBlock extends VertisElement {

    @Name("Кнопка «Подробнее / Скрыть»")
    @FindBy(".//span[contains(@class, 'WalkInCollapseBlock__collapseButton')]")
    VertisElement collapseButton();
}
