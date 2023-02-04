package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface VinBlock extends Block {

    @Name("Госномер")
    @FindBy(".//div[contains(@class, 'gos-number__series-number')]/input")
    VertisElement plateNumber();

    @Name("Регион госномера")
    @FindBy(".//div[contains(@class, 'gos-number__region')]/input")
    VertisElement plateRegion();
}