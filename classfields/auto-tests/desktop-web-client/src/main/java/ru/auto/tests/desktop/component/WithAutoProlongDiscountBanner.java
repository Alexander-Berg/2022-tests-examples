package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.AutoProlongDiscountBanner;

public interface WithAutoProlongDiscountBanner {

    @Name("Баннер «Продлите размещение со скидкой»")
    @FindBy(".//div[contains(@class, 'PlacementAutoProlongationExpireNotice ')]")
    AutoProlongDiscountBanner autoProlongDiscountBanner();
}
