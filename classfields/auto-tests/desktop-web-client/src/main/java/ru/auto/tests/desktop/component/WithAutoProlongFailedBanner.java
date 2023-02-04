package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.AutoProlongFailedBanner;

public interface WithAutoProlongFailedBanner {

    @Name("Баннер «Не удалось включить продление»")
    @FindBy(".//div[contains(@class, 'PlacementAutoProlongationInactiveNotice')]")
    AutoProlongFailedBanner autoProlongFailedBanner();
}
