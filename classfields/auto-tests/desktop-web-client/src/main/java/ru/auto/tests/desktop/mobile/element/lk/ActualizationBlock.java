package ru.auto.tests.desktop.mobile.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 20.02.18
 */
public interface ActualizationBlock extends VertisElement {

    @Name("Кнопка в неактуальном состоянии")
    @FindBy(".//div[@class = 'ButtonActualizeLK']")
    VertisElement notActualButton();

    @Name("Кнопка в актуальном состоянии")
    @FindBy(".//div[@class = 'ButtonActualizeLK ButtonActualizeLK_fresh']")
    VertisElement actualButton();

    @Name("Кнопка «?»")
    @FindBy(".//span[contains(@class, 'ButtonActualizeLK__icon')]")
    VertisElement infoButton();
}