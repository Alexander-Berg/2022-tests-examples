package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.CheckWalletBanner;

public interface WithCheckWalletBanner {

    @Name("Баннер «Проверьте кошелёк»")
    @FindBy(".//div[contains(@class, 'PlacementAutoProlongationWalletNotice')]")
    CheckWalletBanner checkWalletBanner();
}
