package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.AutoProlongInfo;

public interface WithAutoProlongInfo {

    @Name("Плашка «Информация об автопродлении»")
    @FindBy(".//div[contains(@class, 'PlacementProlongationInfoDesktop')] | " +
            ".//div[contains(@class, 'PlacementProlongationInfoLk')]")
    AutoProlongInfo autoProlongInfo();
}
